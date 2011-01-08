package com.yhsif.purge;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class PurgeActivity extends Activity
	implements View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

	public static final int DEFAULT_DAYS = 30;

	public static final String KEY_DAYS = "days";
	public static final String KEY_CALL_LOG = "call_log";
	public static final String KEY_SMS = "sms";
	public static final String KEY_LOCKED_SMS = "locked_sms";

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		SharedPreferences settings = getPreferences(0);
		((EditText)findViewById(R.id.days))
			.setText(Long.toString(settings.getLong(KEY_DAYS, DEFAULT_DAYS)));
		((CheckBox)findViewById(R.id.call_log))
			.setChecked(settings.getBoolean(KEY_CALL_LOG, true));
		((CheckBox)findViewById(R.id.sms))
			.setChecked(settings.getBoolean(KEY_SMS, true));
		((CheckBox)findViewById(R.id.locked_sms))
			.setEnabled(settings.getBoolean(KEY_SMS, true));
		((CheckBox)findViewById(R.id.locked_sms))
			.setChecked(settings.getBoolean(KEY_LOCKED_SMS, false));

		((Button)findViewById(R.id.do_it)).setOnClickListener(this);
		((Button)findViewById(R.id.close)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.sms)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.locked_sms)).setOnClickListener(this);
	}

	@Override public void onStop() {
		super.onStop();
		SharedPreferences settings = getPreferences(0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(KEY_DAYS, getDays());
		editor.putBoolean(KEY_CALL_LOG, isChecked(R.id.call_log));
		editor.putBoolean(KEY_SMS, isChecked(R.id.sms));
		editor.putBoolean(KEY_LOCKED_SMS, isChecked(R.id.locked_sms));
		editor.commit();
	}

	protected int getDays() {
		try {
			return Integer.parseInt(((EditText)findViewById(R.id.days)).getText().toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	protected boolean isChecked(int id) {
		return ((CheckBox)findViewById(id)).isChecked();
	}

	protected void purge() {
		boolean call_log = isChecked(R.id.call_log);
		boolean sms = isChecked(R.id.sms);
		boolean locked_sms = isChecked(R.id.locked_sms);
		int days = getDays();
		int callsDeleted = 0, smsDeleted = 0;

		Time now = new Time();
		now.setToNow();
		now.hour = 0;
		now.minute = 0;
		now.second = 0;
		now.monthDay -= days;
		long time = now.toMillis(true);

		if (call_log) {
			String where = new StringBuilder(Calls.DATE)
				.append(" < ")
				.append(time)
				.toString();
			callsDeleted = getContentResolver().delete(Calls.CONTENT_URI, where, null);
		}

		String message;

		if (call_log)
			if (sms)
				// both
				message = String.format(getString(R.string.both_deleted), callsDeleted, smsDeleted);
			else
				// only calls
				message = String.format(getString(R.string.call_deleted), callsDeleted);
		else
			if (sms)
				// only sms
				message = String.format(getString(R.string.sms_deleted), smsDeleted);
			else
				// neither
				message = getString(R.string.nothing);

		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}

	// for View.OnClickListener
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.close:
				this.finish();
				return;
			case R.id.sms:
				((CheckBox)findViewById(R.id.locked_sms)).setEnabled(isChecked(R.id.sms));
				return;
			case R.id.locked_sms:
				{
					if(!isChecked(R.id.locked_sms)) {
						// do nothing
						return;
					}
					Builder builder = new Builder(this);
					builder.setMessage(R.string.confirm_locked)
						.setOnCancelListener(this)
						.setPositiveButton(R.string.ok, this)
						.setNegativeButton(R.string.cancel, this)
						.show();
				}
				return;
			case R.id.do_it:
				purge();
				return;
		}
	}

	// for DialogInterface.OnClickListener
	public void onClick(DialogInterface dialog, int which) {
		CheckBox check = (CheckBox)findViewById(R.id.locked_sms);
		switch (which){
			case DialogInterface.BUTTON_POSITIVE:
				check.setChecked(true);
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				check.setChecked(false);
				break;
		}
	}

	// for DialogInterface.OnCancelListener
	public void onCancel(DialogInterface dialog) {
		((CheckBox)findViewById(R.id.locked_sms)).setChecked(false);
	}

}
