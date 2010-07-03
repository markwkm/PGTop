package org.postgresql.top;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PGTop extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button startButton = (Button) findViewById(R.id.start);
		final EditText hostEditText = (EditText) findViewById(R.id.host);
		final EditText portEditText = (EditText) findViewById(R.id.port);
		final EditText dbnameEditText = (EditText) findViewById(R.id.dbname);
		final EditText userEditText = (EditText) findViewById(R.id.user);
		final EditText passwordEditText = (EditText) findViewById(R.id.password);

		startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setContentView(R.layout.display);

				String PGHost = hostEditText.getText().toString();
				String PGPort = portEditText.getText().toString();
				String DBName = dbnameEditText.getText().toString();
				String PGUser = userEditText.getText().toString();
				String PGPassword = passwordEditText.getText().toString();

				TextView connectionsTextView = (TextView) findViewById(R.id.connections);

				try {
					Class.forName("org.postgresql.Driver");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

				String url = "jdbc:postgresql:";
				if (PGHost.length() > 0) {
					url += "//" + PGHost;
					if (PGPort.length() > 0) {
						url += ":" + PGPort;
					}
					url += "/";
				}
				url += DBName;

				try {
					Connection conn = DriverManager.getConnection(url, PGUser, PGPassword);

					Statement st = conn.createStatement();
					ResultSet rs = st
							.executeQuery("SELECT numbackends FROM pg_stat_database WHERE datname = '"
									+ DBName + "'");
					if (rs.next()) {
						connectionsTextView.setText("Database Connections: "
								+ rs.getString(1));
					} else {
						connectionsTextView.setText("Database Connections:");
					}
					rs.close();
					st.close();

					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}