package org.postgresql.top;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
	private MenuInflater inflater;

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

	private Boolean hasError;
	private String errorMessage;

	private SharedPreferences preferences;
	private int refreshRate;

	private Connection conn = null;
	private Statement st;
	private PreparedStatement ps;
	private ResultSet rs;

	private static final String sql1 = ""
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

	// Don't show this query if it's the only run running on the system,
	// and only query for SQL running against the database we're
	// currently connected to.

	private static String sql2 = ""
		+ "SELECT NOW() - query_start, current_query "
		+ "FROM pg_stat_activity " + "WHERE datname = ? "
		+ "  AND current_query <> '<IDLE>' "
		+ "  AND current_query <> '<IDLE> in transaction' "
		+ "  AND procpid <> PG_BACKEND_PID() "
		+ "ORDER BY 1 DESC "
		+ "LIMIT 1;";

	private void getActivityStats() throws SQLException {
		try {
			conn = DriverManager.getConnection(url, pgUser, pgPassword);
			st = conn.createStatement();
			rs = st.executeQuery(sql1);
			if (rs.next()) {
				headerString = pgDatabase + " " + rs.getString(1);
				idleConnections = rs.getLong(2);
				idleTransactions = rs.getLong(3);
				waiting = rs.getLong(4);
			}
			rs.close();

			ps = conn.prepareStatement(sql2);
			ps.setString(1, pgDatabase);
			rs = ps.executeQuery();
			if (rs.next()) {
				queryTimeString = rs.getString(1);
				currentQueryString = rs.getString(2);
			} else {
				queryTimeString = "No SQL statements currently running...";
				currentQueryString = "";
			}

			rs.close();
			ps.close();
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

		preferences = getSharedPreferences(PGTop.PREFS_PGTOP, 0);
		pgDatabase = preferences.getString(PGTop.KEY_PGDATABASE, "");
		url = preferences.getString(PGTop.KEY_PGURL, "");
		pgUser = preferences.getString(PGTop.KEY_PGUSER, "");
		pgPassword = preferences.getString(PGTop.KEY_PGPASSWORD, "");

		refreshRate = preferences.getInt(PGTop.KEY_REFRESH,
				PGTop.DEFAULT_REFRESH) * 1000;

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
		inflater = getMenuInflater();
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
			setResult(RESULT_OK, new Intent());
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
		// Loop to refresh the display of activity statistics. Open and close a
		// connection on each loop.
		while (state == State.RUNNING) {
			try {
				getActivityStats();
				hasError = false;
			} catch (SQLException e) {
				errorMessage = e.toString();
				hasError = true;
				state = State.PAUSED;
			}

			handler.sendEmptyMessage(0);

			try {
				Thread.sleep(refreshRate);
			} catch (InterruptedException e) {
				errorMessage = e.toString();
				hasError = true;
				handler.sendEmptyMessage(0);
				state = State.PAUSED;
			}
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			headerTextView.setText(headerString);

			if (hasError) {
				Toast.makeText(PGStatActivity.this, errorMessage,
						Toast.LENGTH_LONG).show();
			} else {
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
		}
	};
}