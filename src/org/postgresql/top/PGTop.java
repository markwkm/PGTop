package org.postgresql.top;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class PGTop extends Activity implements OnClickListener,
		OnItemSelectedListener {
	public static final String PREFS_PGTOP = "PGTopPrefs";

	public static final String KEY_PGDATABASE = "DatabaseKey";
	public static final String KEY_PGURL = "UrlKey";
	public static final String KEY_PGUSER = "UserKey";
	public static final String KEY_PGPASSWORD = "PasswordKey";

	public static final String KEY_CONNECTION = "ConnectionKey";
	public static final String KEY_REFRESH = "RefreshKey";

	public static int DEFAULT_REFRESH = 2;

	public static enum State {
		RUNNING, PAUSED, EXITING;
	}

	private Intent myIntent;

	private Spinner connectionSpinner;
	private ArrayAdapter<CharSequence> connectionAdapter;
	private String selectedItem;

	private PGConnectionOpenHelper openHelper;
	private SQLiteDatabase db;
	private Cursor cursor;

	private String pgHost;
	private String pgPort;
	private String pgDatabase;
	private String pgUser;
	private String pgPassword;
	private int ssl;
	private String url;

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	private int connectionPosition;

	private final Pattern pattern1 = Pattern
			.compile("(.*):(.*)/(.*) \\[(.*)\\] \\((.*)\\)");
	private final Pattern pattern2 = Pattern
			.compile("(.*)/(.*) \\[(.*)\\] \\((.*)\\)");
	private Matcher matcher;

	public void onClick(View view) {
		selectedItem = (String) connectionSpinner.getSelectedItem();
		if (selectedItem == null)
			return;

		matcher = pattern1.matcher(selectedItem);
		if (matcher.find()) {
			pgHost = matcher.group(1);
			pgPort = matcher.group(2);
			pgDatabase = matcher.group(3);
			pgUser = matcher.group(4);
			ssl = (matcher.group(5).equals("SSL") ? 1 : 0);
		} else {
			matcher = pattern2.matcher(selectedItem);
			if (matcher.find()) {
				pgHost = matcher.group(1);
				pgPort = "";
				pgDatabase = matcher.group(2);
				pgUser = matcher.group(3);
				ssl = (matcher.group(4).equals("SSL") ? 1 : 0);
			} else {
				Toast.makeText(PGTop.this,
						"Cannot parse this connection string correctly.",
						Toast.LENGTH_LONG).show();
				return;
			}
		}

		// Build the JDBC connection string.
		url = "jdbc:postgresql:";

		url += "//" + pgHost;
		if (pgPort.length() > 0) {
			url += ":" + pgPort;
		}
		url += "/";
		url += pgDatabase;

		if (ssl == 1) {
			url += "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
		}

		openHelper = new PGConnectionOpenHelper(getApplicationContext());
		db = openHelper.getReadableDatabase();

		final String SELECT_PASSWORD = "SELECT password " + "FROM "
				+ PGConnectionOpenHelper.TABLE_NAME + " WHERE host = '"
				+ pgHost + "' AND port = '" + pgPort + "' AND database = '"
				+ pgDatabase + "' AND user = '" + pgUser + "' AND ssl = "
				+ Integer.toString(ssl) + ";";
		cursor = db.rawQuery(SELECT_PASSWORD, null);
		// FIXME: Handle the event that more than 1 password comes back. But if
		// that situation ever occurs, I think it really is the fault of how
		// data is being inserted into the database.
		if (cursor.getCount() > 0) {
			cursor.moveToNext();
			pgPassword = cursor.getString(0);
		} else {
			Toast.makeText(PGTop.this,
					"Unexplanable problem retrieving database password...",
					Toast.LENGTH_LONG).show();
			cursor.close();
			db.close();
			return;
		}
		cursor.close();
		db.close();

		// Save the database connection variables to be used by StatDisplay
		// class.
		editor.putString(KEY_PGDATABASE, pgDatabase);
		editor.putString(KEY_PGURL, url);
		editor.putString(KEY_PGUSER, pgUser);
		editor.putString(KEY_PGPASSWORD, pgPassword);
		editor.commit();

		myIntent = null;
		switch (view.getId()) {
		case R.id.activity:
			myIntent = new Intent(view.getContext(), PGStatActivity.class);
			break;
		case R.id.database:
			myIntent = new Intent(view.getContext(), PGStatDatabase.class);
			break;
		case R.id.bgwriter:
			myIntent = new Intent(view.getContext(), PGStatBgwriter.class);
			break;
		}
		startActivityForResult(myIntent, 0);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button activityButton = (Button) findViewById(R.id.activity);
		activityButton.setOnClickListener(this);
		final Button bgwriterButton = (Button) findViewById(R.id.bgwriter);
		bgwriterButton.setOnClickListener(this);
		final Button databaseButton = (Button) findViewById(R.id.database);
		databaseButton.setOnClickListener(this);

		preferences = getSharedPreferences(PREFS_PGTOP, 0);
		editor = preferences.edit();

		connectionPosition = preferences.getInt(KEY_CONNECTION, 0);
		connectionSpinner = (Spinner) findViewById(R.id.connection);
		connectionSpinner.setPrompt("Choose a connection");
		connectionAdapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
		connectionSpinner.setAdapter(connectionAdapter);
		connectionSpinner.setSelection(connectionPosition);
		connectionSpinner.setOnItemSelectedListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		connectionPosition = connectionSpinner.getSelectedItemPosition();
		editor.putInt(KEY_CONNECTION, connectionPosition);
		editor.commit();
	}

	public void onNothingSelected(AdapterView<?> parent) {
		// Nothing to do here, should always be something selected.
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.exit:
			System.exit(0);
			return true;
		case R.id.settings:
			startActivityForResult(new Intent(getApplicationContext(),
					PGSettings.class), 0);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		PGConnectionOpenHelper.populateConnectionSpinner(connectionSpinner,
				connectionAdapter, getApplicationContext());

		connectionPosition = preferences.getInt(KEY_CONNECTION, 0);
		connectionSpinner.setSelection(connectionPosition);
	}
}
