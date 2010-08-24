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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class PGTop extends Activity implements OnClickListener {
	public static final String PREFS_REFRESH = "RefreshPrefs";
	public static final String KEY_REFRESH = "RefreshKey";

	public static int DEFAULT_REFRESH = 2;

	public enum State {
		RUNNING, PAUSED, EXITING
	};

	private Spinner connectionSpinner;
	private ArrayAdapter<CharSequence> spinnerAdapter;

	private String pgHost;
	private String pgPort;
	private String pgDatabase;
	private String pgUser;
	private String pgPassword;
	private int ssl;

	public void onClick(View view) {
		String selectedItem = (String) connectionSpinner.getSelectedItem();

		Pattern pattern = Pattern
				.compile("(.*):(.*)/(.*) \\[(.*)\\] \\((.*)\\)");
		Matcher matcher = pattern.matcher(selectedItem);

		if (matcher.find()) {
			pgHost = matcher.group(1);
			pgPort = matcher.group(2);
			pgDatabase = matcher.group(3);
			pgUser = matcher.group(4);
			ssl = (matcher.group(5).equals("SSL") ? 1 : 0);
		} else {
			pattern = Pattern.compile("(.*)/(.*) \\[(.*)\\] \\((.*)\\)");
			matcher = pattern.matcher(selectedItem);
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
		String url = "jdbc:postgresql:";

		url += "//" + pgHost;
		if (pgPort.length() > 0) {
			url += ":" + pgPort;
		}
		url += "/";
		url += pgDatabase;

		if (ssl == 1) {
			url += "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
		}

		PGConnectionOpenHelper openHelper = new PGConnectionOpenHelper(
				getApplicationContext());
		SQLiteDatabase db = openHelper.getReadableDatabase();
		final String SELECT_PASSWORD = "SELECT password " + "FROM "
				+ PGConnectionOpenHelper.TABLE_NAME + " WHERE host = '"
				+ pgHost + "' AND port = '" + pgPort + "' AND database = '"
				+ pgDatabase + "' AND user = '" + pgUser + "' AND ssl = "
				+ Integer.toString(ssl) + ";";
		Cursor c = db.rawQuery(SELECT_PASSWORD, null);
		// FIXME: Handle the event that more than 1 password comes back. But if
		// that situation ever occurs, I think it really is the fault of how
		// data is being inserted into the database.
		if (c.getCount() > 0) {
			c.moveToNext();
			pgPassword = c.getString(0);
		} else {
			Toast.makeText(PGTop.this,
					"Unexplanable problem retrieving database password...",
					Toast.LENGTH_LONG).show();
			c.close();
			db.close();
			return;
		}
		c.close();
		db.close();

		// Save the database connection variables to be used by StatDisplay
		// class.
		SharedPreferences preferences = getSharedPreferences("PGTopPrefs", 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("pgdatabase", pgDatabase);
		editor.putString("pgurl", url);
		editor.putString("pguser", pgUser);
		editor.putString("pgpassword", pgPassword);
		editor.commit();

		Intent myIntent = null;
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

		connectionSpinner = (Spinner) findViewById(R.id.connection);
		connectionSpinner.setPrompt("Choose a connection");
		spinnerAdapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
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
				spinnerAdapter, getApplicationContext());
	}
}