package org.postgresql.top;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class PGRemoveDatabase extends Activity implements OnClickListener {
	private Spinner connectionSpinner;
	private ArrayAdapter<CharSequence> spinnerAdapter;

	private final Pattern pattern1 = Pattern
			.compile("(.*):(.*)/(.*) \\[(.*)\\] \\((.*)\\)");
	private final Pattern pattern2 = Pattern
			.compile("(.*)/(.*) \\[(.*)\\] \\((.*)\\)");
	private Matcher matcher;
	private String selectedItem;

	private PGConnectionOpenHelper openHelper;
	private SQLiteDatabase db;

	private String host;
	private String port;
	private String database;
	private String user;
	private String ssl;

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.remove:
			selectedItem = (String) connectionSpinner.getSelectedItem();
			if (selectedItem == null)
				return;

			matcher = pattern1.matcher(selectedItem);
			if (matcher.find()) {
				host = matcher.group(1);
				port = matcher.group(2);
				database = matcher.group(3);
				user = matcher.group(4);
				ssl = (matcher.group(5).equals("SSL") ? "1" : "0");
			} else {
				matcher = pattern2.matcher(selectedItem);
				if (matcher.find()) {
					host = matcher.group(1);
					port = "";
					database = matcher.group(2);
					user = matcher.group(3);
					ssl = (matcher.group(4).equals("SSL") ? "1" : "0");
				} else {
					Toast.makeText(PGRemoveDatabase.this,
							"Cannot figure out how to remove this connection.",
							Toast.LENGTH_LONG).show();
					return;
				}
			}

			openHelper = new PGConnectionOpenHelper(getApplicationContext());
			db = openHelper.getWritableDatabase();

			final String DELETE_CONNECTION = "DELETE FROM "
					+ PGConnectionOpenHelper.TABLE_NAME + " WHERE host = '"
					+ host + "' AND port = '" + port + "' AND database = '"
					+ database + "' AND user = '" + user + "' AND ssl = " + ssl
					+ ";";
			db.execSQL(DELETE_CONNECTION);
			db.close();

			PGConnectionOpenHelper.populateConnectionSpinner(connectionSpinner,
					spinnerAdapter, getApplicationContext());
			break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.db_remove);

		final Button removeButton = (Button) findViewById(R.id.remove);
		removeButton.setOnClickListener(this);

		connectionSpinner = (Spinner) findViewById(R.id.connection);
		connectionSpinner.setPrompt("Choose a connection");
		spinnerAdapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PGConnectionOpenHelper.populateConnectionSpinner(connectionSpinner,
				spinnerAdapter, getApplicationContext());
	}
}
