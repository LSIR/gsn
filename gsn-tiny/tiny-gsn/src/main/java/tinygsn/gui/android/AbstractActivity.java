/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/gui/android/TinyGSN.java
 *
 * @author Schaer Marc
 */

package tinygsn.gui.android;

import android.app.Activity;
import android.os.Bundle;

public abstract class AbstractActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	protected void onResume() {
		super.onResume();
		TinyGSN.setCurrentActivity(this);
	}
	protected void onPause() {
		clearReferences();
		super.onPause();
	}
	protected void onDestroy() {
		clearReferences();
		super.onDestroy();
	}
	private void clearReferences(){
		Activity currActivity = TinyGSN.getCurrentActivity();
		if (currActivity != null && currActivity.equals(this))
			TinyGSN.setCurrentActivity(null);
	}
}
