package org.postgresql.top;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class PGConnectionOpenHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "connection.db";
	private static final int DATABASE_VERSION = 1;
	public static final String TABLE_NAME = "connection";
	private static final String CREATE_TABLE = "CREATE TABLE "
			+ TABLE_NAME
			+ " (host TEXT, port INTEGER, database TEXT, user TEXT, password TEXT, ssl INTEGER, "
			+ "PRIMARY KEY (host, port, database, user, ssl));";

	public static final String SELECT_CONNECTIONS = "SELECT host, port, database, user, ssl FROM "
			+ PGConnectionOpenHelper.TABLE_NAME
			+ " ORDER BY host, port, database, user, ssl;";

	public PGConnectionOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public final static String getConnectionLabel(Cursor c) {
		final int host = c.getColumnIndex("host");
		final int port = c.getColumnIndex("port");
		final int database = c.getColumnIndex("database");
		final int user = c.getColumnIndex("user");
		final int ssl = c.getColumnIndex("ssl");
		String tmpPortString;

		final String portString = c.getString(port);
		if (portString.length() > 0) {
			tmpPortString = ":" + portString;
		} else {
			tmpPortString = "";
		}

		return c.getString(host) + tmpPortString + "/" + c.getString(database)
				+ " [" + c.getString(user) + "] ("
				+ (c.getInt(ssl) == 1 ? "SSL" : "CLEAR") + ")";
	}

	public final static void populateConnectionSpinner(
			Spinner connectionSpinner,
			ArrayAdapter<CharSequence> spinnerAdapter, Context context) {
		spinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		connectionSpinner.setAdapter(spinnerAdapter);

		spinnerAdapter.clear();

		PGConnectionOpenHelper openHelper = new PGConnectionOpenHelper(context);
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor c = db.rawQuery(PGConnectionOpenHelper.SELECT_CONNECTIONS, null);
		while (c.moveToNext()) {
			spinnerAdapter.add(PGConnectionOpenHelper.getConnectionLabel(c));
		}
		c.close();
		db.close();
	}
}