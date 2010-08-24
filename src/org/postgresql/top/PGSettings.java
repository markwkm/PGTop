package org.postgresql.top;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class PGSettings extends Activity implements OnClickListener,
		OnItemSelectedListener {
	private SharedPreferences settings;
	private int refreshRate;
	private Spinner refreshSpinner;

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.add:
			startActivityForResult(new Intent(view.getContext(),
					PGAddDatabase.class), 0);
			break;
		case R.id.remove:
			startActivityForResult(new Intent(view.getContext(),
					PGRemoveDatabase.class), 0);
			break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		// Restore refresh rate preferences, the position is the same as the
		// value - 1.
		settings = getSharedPreferences(PGTop.PREFS_REFRESH, 0);
		refreshRate = settings.getInt(PGTop.KEY_REFRESH, PGTop.DEFAULT_REFRESH);

		refreshSpinner = (Spinner) findViewById(R.id.refresh);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.rates, android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		refreshSpinner.setAdapter(adapter);
		refreshSpinner.setSelection(refreshRate - 1);
		refreshSpinner.setOnItemSelectedListener(this);

		final Button addButton = (Button) findViewById(R.id.add);
		addButton.setOnClickListener(this);
		final Button removeButton = (Button) findViewById(R.id.remove);
		removeButton.setOnClickListener(this);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		SharedPreferences.Editor editor = settings.edit();
		refreshRate = Integer.parseInt((String) refreshSpinner
				.getSelectedItem());
		editor.putInt(PGTop.KEY_REFRESH, refreshRate);
		editor.commit();
	}

	public void onNothingSelected(AdapterView<?> parent) {
		// Nothing to do here, should always be something selected.
	}
}