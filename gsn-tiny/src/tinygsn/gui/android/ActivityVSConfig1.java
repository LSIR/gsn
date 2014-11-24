/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/gui/android/ActivityVSConfig1.java
*
* @author Do Ngoc Hoan
*/


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
