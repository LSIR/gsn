package tinygsn.gui.android;

import gsn.http.rest.PushDelivery;
import java.util.ArrayList;
import java.util.List;
import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerPublishData;
import tinygsn.model.vsensor.VirtualSensor;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ActivityPublishData extends SherlockFragmentActivity {
	public static String[] STRATEGY = { "On demand", "Periodically (time)",
			"Periodically (values)" };
	static int TEXT_SIZE = 10;
	public static String DEFAULT_SERVER = "http://10.0.2.2:22001";

	private Context context = null;
	private AndroidControllerPublishData controller;
	private Spinner spinner_vsName;
	private Spinner spinner_strategy;
	private EditText serverEditText = null;

	private Handler handlerVS;
	private ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();

	private PushDelivery push;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_publish_data);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(findViewById(R.id.select_server_btn));

		serverEditText = (EditText) findViewById(R.id.editText_server);

		context = this;

		setUpController();
		renderStrategyList();
		registerPush();
	}

	public void setUpController() {
		handlerVS = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				vsList = (ArrayList<VirtualSensor>) msg.obj;
				renderVSList();
			};
		};

		controller = new AndroidControllerPublishData(this);
		controller.setHandlerVS(handlerVS);
		controller.loadListVS();
	}

	public void renderVSList() {
		spinner_vsName = (Spinner) findViewById(R.id.spinner_vsname);
		List<String> list = new ArrayList<String>();

		for (VirtualSensor vs : vsList) {
			list.add(vs.getConfig().getName());
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_vsName.setAdapter(dataAdapter);

		spinner_vsName.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				Toast.makeText(
						parent.getContext(),
						"The VS \"" + parent.getItemAtPosition(pos).toString()
								+ "\" is selected.", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	public void renderStrategyList() {
		spinner_strategy = (Spinner) findViewById(R.id.spinner_strategy);
		List<String> list = new ArrayList<String>();

		for (String s : STRATEGY) {
			list.add(s);
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_strategy.setAdapter(dataAdapter);

		spinner_strategy.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {

				if (pos != 0) {
					Toast.makeText(
							parent.getContext(),
							"The strategy \"" + parent.getItemAtPosition(pos).toString()
									+ "\" has not been implemented!", Toast.LENGTH_SHORT).show();

				}
				else
					Toast.makeText(
							parent.getContext(),
							"The strategy \"" + parent.getItemAtPosition(pos).toString()
									+ "\" is selected.", Toast.LENGTH_SHORT).show();

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Publish").setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
//			Intent myIntent = new Intent(this, ActivityHome.class);
//			this.startActivity(myIntent);
			finish();
			break;
		case 0:
			publish(null);
			break;
		}
		return true;
	}

	public void registerPush() {
		push = new PushDelivery(DEFAULT_SERVER + "/streaming/", 1.235813);
	}

	public void publish(View view) {
		StreamElement se = controller.loadLatestData(spinner_vsName
				.getSelectedItem().toString());

		if (se != null) {
			new PublishDataTask().execute(se);
		}
		else {
			Toast.makeText(this, "VS has no data to publish", Toast.LENGTH_SHORT)
					.show();
		}
	}

	private class PublishDataTask extends AsyncTask<StreamElement, Void, Boolean> {

		private StreamElement se = null;

		@Override
		protected Boolean doInBackground(StreamElement... params) {
			se = params[0];
			return push.writeStreamElement(se);
		}

		protected void onPostExecute(Boolean results) {
			if (results == true)
				Toast.makeText(context, "Published: " + se.toString(),
						Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(context, "Publish fail: " + se.toString(),
						Toast.LENGTH_SHORT).show();
		}
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// //This uses the imported MenuItem from ActionBarSherlock
	// Toast.makeText(this, "Got click: " + item.toString(),
	// Toast.LENGTH_SHORT).show();
	// return true;
	// }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		menu.add("http://10.0.2.2:22001");
		menu.add("http://gsn.ijs.si");
		menu.add("http://montblanc.slf.ch:22001");
		menu.add("http://data.permasense.ch");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		// Note how this callback is using the fully-qualified class name
		Toast.makeText(this, "Got click: " + item.toString(), Toast.LENGTH_SHORT)
				.show();
		serverEditText.setText(item.toString());
		return true;
	}

	public void select_server(View view) {
		view.showContextMenu();
	}
}
