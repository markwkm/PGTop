package org.postgresql.top;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.top.PGTop.State;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class PGStatActivity extends Activity implements Runnable {
	private String pgDatabase;
	private String url;
	private String pgUser;
	private String pgPassword;

	Thread thread;

	private String headerString;
	private long idleConnections = 0;
	private long idleTransactions = 0;
	private long waiting = 0;
	private String currentQueryString;
	private String queryTimeString;

	private TextView headerTextView;
	private TextView idleConnectionsTextView;
	private TextView idleTransactionsTextView;
	private TextView waitingTextView;
	private TextView currentQueryTextView;
	private TextView queryTimeTextView;

	private State state;

	private void getActivityStats() throws SQLException {
		Connection conn = null;
		Statement st;
		ResultSet rs;

		String sql = null;

		try {
			conn = DriverManager.getConnection(url, pgUser, pgPassword);

			st = conn.createStatement();

			sql = ""
					+ "SELECT NOW(), "
					+ "      (SELECT COUNT(*) "
					+ "       FROM pg_stat_activity "
					+ "       WHERE current_query = '<IDLE>') AS idle_connections, "
					+ "      (SELECT COUNT(*) "
					+ "       FROM pg_stat_activity "
					+ "       WHERE current_query = '<IDLE> in transaction') AS idle_transactions, "
					+ "      (SELECT COUNT(*) "
					+ "       FROM pg_stat_activity "
					+ "       WHERE waiting IS TRUE) AS waiting;";
			rs = st.executeQuery(sql);
			if (rs.next()) {
				headerString = pgDatabase + " " + rs.getString(1);
				idleConnections = rs.getLong(2);
				idleTransactions = rs.getLong(3);
				waiting = rs.getLong(4);
			}
			rs.close();

			/*
			 * Don't show this query if it's the only run running on the system,
			 * and only query for SQL running against the database we're
			 * currently connected to.
			 */
			// FIXME: Use named parameters.
			sql = ""
					+ "SELECT NOW() - query_start, current_query "
					+ "FROM pg_stat_activity "
					+ "WHERE datname = '" + pgDatabase + "' "
					+ "  AND current_query <> '<IDLE>' "
					+ "  AND current_query <> '<IDLE> in transaction' "
					+ "  AND procpid <> PG_BACKEND_PID() "
					+ "ORDER BY 1 DESC "
					+ "LIMIT 1;";
			rs = st.executeQuery(sql);
			if (rs.next()) {
				queryTimeString = rs.getString(1);
				currentQueryString = rs.getString(2);
			} else {
				queryTimeString = "No SQL statements currently running...";
				currentQueryString = "";
			}
			rs.close();

			st.close();
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pg_stat_activity);

		state = State.RUNNING;

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			Toast
					.makeText(PGStatActivity.this, e.toString(),
							Toast.LENGTH_LONG).show();
			return;
		}

		SharedPreferences preferences = getSharedPreferences("PGTopPrefs", 0);
		pgDatabase = preferences.getString("pgdatabase", "");
		url = preferences.getString("pgurl", "");
		pgUser = preferences.getString("pguser", "");
		pgPassword = preferences.getString("pgpassword", "");

		headerTextView = (TextView) findViewById(R.id.displayheader);
		idleConnectionsTextView = (TextView) findViewById(R.id.idle_connections);
		idleTransactionsTextView = (TextView) findViewById(R.id.idle_transactions);
		waitingTextView = (TextView) findViewById(R.id.waiting);
		queryTimeTextView = (TextView) findViewById(R.id.query_time);
		currentQueryTextView = (TextView) findViewById(R.id.current_query);

		thread = new Thread(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		state = State.EXITING;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop:
			Intent intent = new Intent();
			setResult(RESULT_OK, intent);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		state = State.PAUSED;
	}

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
		 * Loop to refresh the display of activity statistics. Open and close a
		 * connection on each loop.
		 */
		while (state == State.RUNNING) {
			try {
				getActivityStats();

				handler.sendEmptyMessage(0);
				// FIXME: Make the refresh rate a configuration parameter.
				Thread.sleep(2000);
			} catch (SQLException e) {
				Toast.makeText(PGStatActivity.this, e.toString(),
						Toast.LENGTH_LONG).show();
				return;
			} catch (InterruptedException e) {
				Toast.makeText(PGStatActivity.this, e.toString(),
						Toast.LENGTH_LONG).show();
				return;
			}
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			headerTextView.setText(headerString);
			idleConnectionsTextView.setText("Idle Connections: "
					+ Long.toString(idleConnections));
			idleTransactionsTextView.setText("Idle Transactions: "
					+ Long.toString(idleTransactions));
			waitingTextView.setText("Connections Waiting: "
					+ Long.toString(waiting));
			queryTimeTextView.setText("Longest Running Query: "
					+ queryTimeString);
			currentQueryTextView.setText(currentQueryString);
		}
	};
}