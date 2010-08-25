package org.postgresql.top;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class PGStatBgwriter extends Activity implements Runnable {
	private String pgDatabase;
	private String url;
	private String pgUser;
	private String pgPassword;

	Thread thread = null;

	private String headerString;
	private long checkpointsTimed = 0;
	private long checkpointsReq = 0;
	private long buffersCheckpoint = 0;
	private long buffersClean = 0;
	private long maxwrittenClean = 0;
	private long buffersBackend = 0;
	private long buffersAlloc = 0;

	private long checkpointsTimedOld = 0;
	private long checkpointsReqOld = 0;
	private long buffersCheckpointOld = 0;
	private long buffersCleanOld = 0;
	private long maxwrittenCleanOld = 0;
	private long buffersBackendOld = 0;

	private TextView headerTextView;
	private TextView checkpointsTimedTextView;
	private TextView checkpointsReqTextView;
	private TextView buffersCheckpointTextView;
	private TextView buffersCleanTextView;
	private TextView maxwrittenCleanTextView;
	private TextView buffersBackendTextView;
	private TextView buffersAllocTextView;

	private State state;

	private Boolean hasError;
	private String errorMessage;

	private Connection conn = null;
	private Statement st;
	private ResultSet rs;

	private int major, branch;

	private SharedPreferences preferences;
	private int refreshRate;

	private static final String sql = ""
			+ "SELECT NOW(), checkpoints_timed, checkpoints_req, "
			+ "       buffers_checkpoint, buffers_clean, "
			+ "       maxwritten_clean, buffers_backend, "
			+ "       buffers_alloc " + "FROM pg_stat_bgwriter;";

	private void getBgwriterStats() throws SQLException {
		try {
			conn = DriverManager.getConnection(url, pgUser, pgPassword);

			st = conn.createStatement();
			rs = st.executeQuery(sql);
			if (rs.next()) {
				// Save old values.
				checkpointsTimedOld = checkpointsTimed;
				checkpointsReqOld = checkpointsReq;
				buffersCheckpointOld = buffersCheckpoint;
				buffersCleanOld = buffersClean;
				maxwrittenCleanOld = maxwrittenClean;
				buffersBackendOld = buffersBackend;

				// Get new values.
				headerString = pgDatabase + " " + rs.getString(1);
				checkpointsTimed = rs.getLong(2);
				checkpointsReq = rs.getLong(3);
				buffersCheckpoint = rs.getLong(4);
				buffersClean = rs.getLong(5);
				maxwrittenClean = rs.getLong(6);
				buffersBackend = rs.getLong(7);
				buffersAlloc = rs.getLong(8);
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
		setContentView(R.layout.pg_stat_bgwriter);

		state = State.RUNNING;

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			Toast
					.makeText(PGStatBgwriter.this, e.toString(),
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
		checkpointsTimedTextView = (TextView) findViewById(R.id.checkpoints_timed);
		checkpointsReqTextView = (TextView) findViewById(R.id.checkpoints_req);
		buffersCheckpointTextView = (TextView) findViewById(R.id.buffers_checkpoint);
		buffersCleanTextView = (TextView) findViewById(R.id.buffers_clean);
		maxwrittenCleanTextView = (TextView) findViewById(R.id.maxwritten_clean);
		buffersBackendTextView = (TextView) findViewById(R.id.buffers_backend);
		buffersAllocTextView = (TextView) findViewById(R.id.buffers_alloc);

		try {
			conn = DriverManager.getConnection(url, pgUser, pgPassword);

			st = conn.createStatement();
			rs = st.executeQuery("SHOW server_version;");

			if (rs.next()) {
				final Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+).*");
				final Matcher matcher = pattern.matcher(rs.getString(1));
				if (matcher.find()) {
					major = Integer.parseInt(matcher.group(1));
					branch = Integer.parseInt(matcher.group(2));
				}
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			Toast
					.makeText(PGStatBgwriter.this, e.toString(),
							Toast.LENGTH_LONG).show();
			return;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// Nothing to worry about now.
				}
			}
		}

		if (major == 8 && branch < 3) {
			// This should be all cases older than 8.4, and there will be
			// nothing to get to display.
			headerTextView.setText("Not supported in PostgreSQL " + major + "."
					+ branch);
		} else {
			thread = new Thread(this);
		}
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
		if (thread != null)
			thread.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		state = State.EXITING;
	}

	public void run() {
		// Loop to refresh the display of background writer statistics. Open and
		// close a connection on each loop.
		while (state == State.RUNNING) {
			try {
				getBgwriterStats();
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
				Toast.makeText(PGStatBgwriter.this, errorMessage,
						Toast.LENGTH_LONG).show();
			} else {
				checkpointsTimedTextView
						.setText("Scheduled Checkpoints: "
								+ Long.toString(checkpointsTimed
										- checkpointsTimedOld));
				checkpointsReqTextView.setText("Requested Checkpoints: "
						+ Long.toString(checkpointsReq - checkpointsReqOld));
				buffersCheckpointTextView
						.setText("Buffers Written by Checkpoint: "
								+ Long.toString(buffersCheckpoint
										- buffersCheckpointOld));
				buffersCleanTextView.setText("Buffers Cleaned: "
						+ Long.toString(buffersClean - buffersCleanOld));
				maxwrittenCleanTextView
						.setText("Times Background Writer Stopped: "
								+ Long.toString(maxwrittenClean
										- maxwrittenCleanOld));
				buffersBackendTextView.setText("Buffer Written by Backends: "
						+ Long.toString(buffersBackend - buffersBackendOld));
				buffersAllocTextView.setText("Total Buffers Allocated: "
						+ Long.toString(buffersAlloc));
			}
		}
	};
}