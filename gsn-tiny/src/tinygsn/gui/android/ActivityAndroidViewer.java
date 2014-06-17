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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityAndroidViewer.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerDemo;
import tinygsn.gui.AbstractViewer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ActivityAndroidViewer extends Activity implements AbstractViewer {

	TextView lblOutput = null;
	Handler handler;

	AndroidControllerDemo controller = new AndroidControllerDemo(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);
		
		lblOutput = (TextView) findViewById(R.id.txbOutput);

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				StreamElement se = (StreamElement) msg.obj;
				if (se == null)
					lblOutput.setText("No signal!");
				else
					lblOutput.setText(se.toString());
			}
		};

		controller.setHandler(handler);

		controller.startLoadVSList();
	}

	public void showDataDemo(StreamElement streamElement) {
		lblOutput.setText(streamElement.toString());
	}

	@Override
	public void displayAlert() {
	}

	@Override
	public void updateListVS() {
	}
	
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  	menu.add(Menu.NONE, 0, 0, "Settings");
  	menu.add(Menu.NONE, 1, 0, "View Data");
  	
  	return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	switch (item.getItemId()) {
  		case 0:
  			Intent myIntent = new Intent(this, ActivityVSConfig.class);
  			this.startActivity(myIntent);
  			return true;
  		case 1:
  			Intent viewDateIntent = new Intent(this, ActivityViewData.class);
  			this.startActivity(viewDateIntent);
  			return true;
  			
  	}
  	return true;
  }
  
}