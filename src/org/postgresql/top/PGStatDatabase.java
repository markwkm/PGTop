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

public class PGStatDatabase extends Activity implements Runnable {
	private String pgDatabase;
	private String url;
	private String pgUser;
	private String pgPassword;

	private TextView headerTextView;
	private TextView numbackendsTextView;
	private TextView commitsTextView;
	private TextView rollbacksTextView;
	private TextView readTextView;
	private TextView hitTextView;
	private TextView returnedTextView;
	private TextView fetchedTextView;
	private TextView insertedTextView;
	private TextView updatedTextView;
	private TextView deletedTextView;

	private String headerString;
	private long numbackends = 0;
	private long commits = 0;
	private long rollbacks = 0;
	private long read = 0;
	private long hit = 0;
	private long returned = 0;
	private long fetched = 0;
	private long inserted = 0;
	private long updated = 0;
	private long deleted = 0;

	private long commitsOld = 0;
	private long rollbacksOld = 0;
	private long readOld = 0;
	private long hitOld = 0;
	private long returnedOld = 0;
	private long fetchedOld = 0;
	private long insertedOld = 0;
	private long updatedOld = 0;
	private long deletedOld = 0;

	private String readPretty;
	private String hitPretty;

	Thread thread;

	private State state;

	private void getDatabaseStats() throws SQLException {
		Connection conn;
		Statement st;
		ResultSet rs;

		String sql = ""
				+ "SELECT NOW(), numbackends, xact_commit, xact_rollback, "
				+ "          blks_read, blks_hit, tup_returned, tup_fetched, "
				+ "          tup_inserted, tup_updated, tup_deleted, "
				+ "          PG_SIZE_PRETTY((blks_read - "
				+ Long.toString(readOld) + ") * setting), "
				+ "          PG_SIZE_PRETTY((blks_hit - "
				+ Long.toString(hitOld) + ") * setting) "
				+ "FROM (SELECT setting::BIGINT "
				+ "      FROM pg_settings "
				+ "      WHERE name = 'block_size') AS pg_settings, "
				+ "     pg_stat_database "
				+ "WHERE datname = '" + pgDatabase + "';";

		conn = DriverManager.getConnection(url, pgUser, pgPassword);

		st = conn.createStatement();
		rs = st.executeQuery(sql);
		if (rs.next()) {
			/* Save previous values. */
			commitsOld = commits;
			rollbacksOld = rollbacks;
			readOld = read;
			hitOld = hit;
			returnedOld = returned;
			fetchedOld = fetched;
			insertedOld = inserted;
			updatedOld = updated;
			deletedOld = deleted;

			/* Get new values. */
			headerString = pgDatabase + " " + rs.getString(1);
			numbackends = rs.getLong(2);
			commits = rs.getLong(3);
			rollbacks = rs.getLong(4);
			read = rs.getLong(5);
			hit = rs.getLong(6);
			returned = rs.getLong(7);
			fetched = rs.getLong(8);
			inserted = rs.getLong(9);
			updated = rs.getLong(10);
			deleted = rs.getLong(11);
			readPretty = rs.getString(12);
			hitPretty = rs.getString(13);
		}
		rs.close();
		st.close();
		conn.close();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pg_stat_database);

		state = State.RUNNING;

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			Toast
					.makeText(PGStatDatabase.this, e.toString(),
							Toast.LENGTH_LONG).show();
			return;
		}

		SharedPreferences preferences = getSharedPreferences("PGTopPrefs", 0);
		pgDatabase = preferences.getString("pgdatabase", "");
		url = preferences.getString("pgurl", "");
		pgUser = preferences.getString("pguser", "");
		pgPassword = preferences.getString("pgpassword", "");

		headerTextView = (TextView) findViewById(R.id.displayheader);
		numbackendsTextView = (TextView) findViewById(R.id.numbackends);
		commitsTextView = (TextView) findViewById(R.id.xact_commit);
		rollbacksTextView = (TextView) findViewById(R.id.xact_rollback);
		readTextView = (TextView) findViewById(R.id.blks_read);
		hitTextView = (TextView) findViewById(R.id.blks_hit);
		returnedTextView = (TextView) findViewById(R.id.tup_returned);
		fetchedTextView = (TextView) findViewById(R.id.tup_fetched);
		insertedTextView = (TextView) findViewById(R.id.tup_inserted);
		updatedTextView = (TextView) findViewById(R.id.tup_updated);
		deletedTextView = (TextView) findViewById(R.id.tup_deleted);

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
				getDatabaseStats();

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
			numbackendsTextView.setText("Database Connections: "
					+ Long.toString(numbackends));
			commitsTextView.setText("Commits: "
					+ Long.toString(commits - commitsOld));
			rollbacksTextView.setText("Rollbacks: "
					+ Long.toString(rollbacks - rollbacksOld));
			readTextView.setText("Blocks Read: "
					+ Long.toString(read - readOld) + " (" + readPretty + ")");
			hitTextView.setText("Blocks Hit: " + Long.toString(hit - hitOld)
					+ " (" + hitPretty + ")");
			returnedTextView.setText("Rows Returned: "
					+ Long.toString(returned - returnedOld));
			fetchedTextView.setText("Row Fetched: "
					+ Long.toString(fetched - fetchedOld));
			insertedTextView.setText("Rows Inserted: "
					+ Long.toString(inserted - insertedOld));
			updatedTextView.setText("Rows Updated: "
					+ Long.toString(updated - updatedOld));
			deletedTextView.setText("Rows Deleted: "
					+ Long.toString(deleted - deletedOld));
		}
	};
}