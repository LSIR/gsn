package tinygsn.gui.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class ActivityVSConfig1 extends PreferenceActivity {
	private static final String TAG = "AndroidVSConfigSetting";
	
	SharedPreferences sharedPrefs;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.activity_vs_config_preference_style);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	public void saveVS(View view){
		String builder = new String();

		builder = sharedPrefs.getString("vs_name", "-1");
		Log.v(TAG, builder);
		
		builder = sharedPrefs.getString("window_size", "-1");
		Log.v(TAG, builder);
		
		builder = sharedPrefs.getString("step", "-1");
		Log.v(TAG, builder);
		
		builder = sharedPrefs.getString("sampling_rate", "-1");
		Log.v(TAG, builder);
		
		builder = sharedPrefs.getString("sampling_rate", "-1");
		Log.v(TAG, builder);
		
		builder = sharedPrefs.getString("wp_name", "-1");
		Log.v(TAG, builder);
	}
	
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// menu.add(Menu.NONE, 0, 0, "Show current settings");
	// return super.onCreateOptionsMenu(menu);
	// }

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case 0:
	// startActivity(new Intent(this, ShowSettingsActivity.class));
	// return true;
	// }
	// return false;
	// }
}
