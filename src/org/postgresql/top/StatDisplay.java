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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class StatDisplay extends Activity implements OnClickListener {
	public void onClick(View view) {
		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display);

		final Button stopButton = (Button) findViewById(R.id.stop);
		stopButton.setOnClickListener(this);

		TextView headerTextView = (TextView) findViewById(R.id.displayheader);
		TextView connectionsTextView = (TextView) findViewById(R.id.connections);

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			Toast.makeText(StatDisplay.this, e.toString(), Toast.LENGTH_LONG)
					.show();
			return;
		}

		SharedPreferences settings = getSharedPreferences("PGTopPrefs", 0);
		String DBName = settings.getString("dbname", "");
		String url = settings.getString("pgurl", "");
		String PGUser = settings.getString("pguser", "");
		String PGPassword = settings.getString("pgpassword", "");

		try {
			Connection conn;
			Statement st;
			ResultSet rs;

			conn = DriverManager.getConnection(url, PGUser, PGPassword);

			st = conn.createStatement();
			rs = st
					.executeQuery("SELECT NOW(), numbackends FROM pg_stat_database WHERE datname = '"
							+ DBName + "'");
			if (rs.next()) {
				headerTextView.setText(DBName + " " + rs.getString(1));
				connectionsTextView.setText("Database Connections: "
						+ rs.getString(2));
			}
			rs.close();
			st.close();
			conn.close();
		} catch (SQLException e) {
			Toast.makeText(StatDisplay.this, e.toString(), Toast.LENGTH_LONG)
					.show();
			return;
		}
	}
}
