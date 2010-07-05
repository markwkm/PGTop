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
		final EditText hostEditText = (EditText) findViewById(R.id.host);
		final EditText portEditText = (EditText) findViewById(R.id.port);
		final EditText dbnameEditText = (EditText) findViewById(R.id.dbname);
		final EditText userEditText = (EditText) findViewById(R.id.user);
		final EditText passwordEditText = (EditText) findViewById(R.id.password);

		String PGHost = hostEditText.getText().toString();
		String PGPort = portEditText.getText().toString();
		String DBName = dbnameEditText.getText().toString();
		String PGUser = userEditText.getText().toString();
		String PGPassword = passwordEditText.getText().toString();

		String url = "jdbc:postgresql:";
		if (PGHost.length() > 0) {
			url += "//" + PGHost;
			if (PGPort.length() > 0) {
				url += ":" + PGPort;
			}
			url += "/";
		}
		url += DBName;

		SharedPreferences preferences = getSharedPreferences("PGTopPrefs", 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("dbname", DBName);
		editor.putString("pgurl", url);
		editor.putString("pguser", PGUser);
		editor.putString("pgpassword", PGPassword);
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