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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityHelp.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import android.os.Bundle;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class ActivityHelp extends SherlockActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Sherlock___Theme_DarkActionBar);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		String text = "TinyGSN allows you to collect the sensor data accessible through Android-powered devices, process the data according to your customed specification, exchange the data with the Global Sensor Network (GSN) servers."
				+ "\n\n\tMain functionalities:"
				+ "\n\t\t* Collect sensor data: "
				+ "\n\t\t* View and share sensor data: "
				+ "\n\t\t* Publish sensor data to GSN servers: "
				+ "\n\t\t* Get sensor data from GSN servers: ";
		((TextView) findViewById(R.id.text)).setText(text);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			break;
		}
		return true;
	}
}
