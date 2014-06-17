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