package org.postgresql.top;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PGSettings extends Activity implements OnClickListener {
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

		final Button addButton = (Button) findViewById(R.id.add);
		addButton.setOnClickListener(this);
		final Button removeButton = (Button) findViewById(R.id.remove);
		removeButton.setOnClickListener(this);
	}
}