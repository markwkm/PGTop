package org.postgresql.top;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PGStatDatabase extends Activity implements OnClickListener,
		Runnable {
	private String pgDatabase;
	private String url;
	private String pgUser;
	private String pgPassword;

	private TextView headerTextView;
	private TextView connectionsTextView;

	private String headerString;
	private String backendString;

	Thread thread;

	private enum State {
		RUNNING, PAUSED, EXITING
	};

	private State state;

	public void onClick(View view) {
		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pg_stat_database);

		state = State.RUNNING;

		final Button stopButton = (Button) findViewById(R.id.stop);
		stopButton.setOnClickListener(this);

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			Toast
					.makeText(PGStatDatabase.this, e.toString(),
							Toast.LENGTH_LONG).show();
			return;
		}

		SharedPreferences settings = getSharedPreferences("PGTopPrefs", 0);
		pgDatabase = settings.getString("pgdatabase", "");
		url = settings.getString("pgurl", "");
		pgUser = settings.getString("pguser", "");
		pgPassword = settings.getString("pgpassword", "");

		headerTextView = (TextView) findViewById(R.id.displayheader);
		connectionsTextView = (TextView) findViewById(R.id.connections);

		thread = new Thread(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		state = State.EXITING;
	}

	@Override
	protected void onPause() {
		super.onPause();
		state = State.PAUSED;
	}

	@Override
	protected void onResume() {
		super.onResume();
		state = State.RUNNING;
		thread.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		state = State.EXITING;
	}

	public void run() {
		/*
		 * Loop to refresh the display of database statistics. Open and close a
		 * connection on each loop.
		 */
		while (state == State.RUNNING) {
			try {
				Connection conn;
				Statement st;
				ResultSet rs;

				conn = DriverManager.getConnection(url, pgUser, pgPassword);

				st = conn.createStatement();
				rs = st
						.executeQuery("SELECT NOW(), numbackends FROM pg_stat_database WHERE datname = '"
								+ pgDatabase + "'");
				if (rs.next()) {
					headerString = pgDatabase + " " + rs.getString(1);
					backendString = "Database Connections: " + rs.getString(2);
				}
				rs.close();
				st.close();
				conn.close();

				handler.sendEmptyMessage(0);
				// FIXME: Make the refresh rate a configuration parameter.
				Thread.sleep(2000);
			} catch (SQLException e) {
				Toast.makeText(PGStatDatabase.this, e.toString(),
						Toast.LENGTH_LONG).show();
				return;
			} catch (InterruptedException e) {
				Toast.makeText(PGStatDatabase.this, e.toString(),
						Toast.LENGTH_LONG).show();
				return;
			}
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			headerTextView.setText(headerString);
			connectionsTextView.setText(backendString);
		}
	};
}