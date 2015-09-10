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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityAboutUs.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import android.view.MenuItem;

/**
 * About the development team
 * 
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 *
 */
public class ActivityAboutUs extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        ((TextView)findViewById(R.id.text)).setText("About us: " 
        + "\n\n\tThis application is part of Global Sensor Networks (GSN)."
        		+ "\n\t\t\t* Developers: Do Ngoc Hoan, Aida Amini, Julien Eberle \n\t\t\t\t"
        		+ "\n\n\tSource code available at https://github.com/LSIR/gsn/"); 
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
