package org.postgresql.top;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class PGTop extends Activity implements OnClickListener {
	public void onClick(View view) {
		final EditText pghostEditText = (EditText) findViewById(R.id.pghost);
		final EditText pgportEditText = (EditText) findViewById(R.id.pgport);
		final EditText pgdatabaseEditText = (EditText) findViewById(R.id.pgdatabase);
		final EditText pguserEditText = (EditText) findViewById(R.id.pguser);
		final EditText pgpasswordEditText = (EditText) findViewById(R.id.pgpassword);

		String pgHost = pghostEditText.getText().toString();
		String pgPort = pgportEditText.getText().toString();
		String pgDatabase = pgdatabaseEditText.getText().toString();
		String pgUser = pguserEditText.getText().toString();
		String pgPassword = pgpasswordEditText.getText().toString();

		/* Build the JDBC connection string. */
		String url = "jdbc:postgresql:";
		if (pgHost.length() > 0) {
			url += "//" + pgHost;
			if (pgPort.length() > 0) {
				url += ":" + pgPort;
			}
			url += "/";
		}
		url += pgDatabase;

		/*
		 * Save the database connection variables to be used by StatDisplay
		 * class.
		 */
		SharedPreferences preferences = getSharedPreferences("PGTopPrefs", 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("pgdatabase", pgDatabase);
		editor.putString("pgurl", url);
		editor.putString("pguser", pgUser);
		editor.putString("pgpassword", pgPassword);
		editor.commit();

		Intent myIntent = new Intent(view.getContext(), StatDisplay.class);
		startActivityForResult(myIntent, 0);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button startButton = (Button) findViewById(R.id.start);
		startButton.setOnClickListener(this);
	}
}