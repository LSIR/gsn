package tinygsn.gui.android;

import java.util.ArrayList;
import java.util.List;
import tinygsn.controller.AndroidControllerPullData;
import tinygsn.gui.android.utils.DetailedDataFragment;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ActivityPullData extends SherlockFragmentActivity {
	public static String[] SERVER_LIST = { "http://10.0.2.2:22001",
			"http://data.permasense.ch", "http://montblanc.slf.ch:22001",
			"http://gsn.ijs.si" };

	public static String[] RANGE_LIST = { "Latest values", "Range" };
	static int TEXT_SIZE = 10;

	private AndroidControllerPullData controller;
//	private Spinner spinner_server_name;
	private Spinner spinner_vsName;
	private Spinner spinner_rangeType;
	private EditText serverEditText = null;
	private Dialog dialog;
	
	Handler handlerVS;
	Handler handlerData;

	// ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();
	ArrayList<String> vsNameList = new ArrayList<String>();
	String pulledData = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pull);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(findViewById(R.id.select_server_btn));
		serverEditText = (EditText) findViewById(R.id.editText_server);
		
		dialog = new Dialog(this);
		dialog.setContentView(R.layout.dialog_progress_bar);
		dialog.setTitle("Loading VS list");
		
		setUpController();
		// renderServerList();
		renderRangeType();
	}

	@SuppressWarnings("unchecked")
	public void setUpController() {
		handlerVS = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				vsNameList = (ArrayList<String>) msg.obj;
				dialog.dismiss();
				
				renderVSList();
			};
		};

		handlerData = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				pulledData = (String) msg.obj;
				showPulledData(pulledData);
				showDialogDetail(pulledData);
			};
		};

		controller = new AndroidControllerPullData(this);
		controller.setHandlerVS(handlerVS);
		controller.setHandlerData(handlerData);
		controller.loadListVS(serverEditText.getText().toString());
		
	}

	protected void showPulledData(String pulledData) {
		TextView out = (TextView) findViewById(R.id.txbPulledData);
		out.setText(pulledData);
		out.setTextSize(TEXT_SIZE);
	}

	private void showDialogDetail(String pulledData) {
		String out = pulledData;

		DialogFragment newFragment = new DetailedDataFragment(out);
		newFragment.show(getSupportFragmentManager(), "dialog");
	}

	// public void renderServerList() {
	// spinner_server_name = (Spinner) findViewById(R.id.spinner_server_name);
	// List<String> list = new ArrayList<String>();
	//
	// for (String s : SERVER_LIST) {
	// list.add(s);
	// }
	//
	// ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
	// R.layout.spinner_item, list);
	// dataAdapter
	// .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	// spinner_server_name.setAdapter(dataAdapter);
	//
	// spinner_server_name.setOnItemSelectedListener(new OnItemSelectedListener()
	// {
	// public void onItemSelected(AdapterView<?> parent, View view, int pos,
	// long id) {
	// Toast.makeText(
	// parent.getContext(),
	// "The server \"" + parent.getItemAtPosition(pos).toString()
	// + "\" is selected.", Toast.LENGTH_SHORT).show();
	//
	// controller.loadListVS(spinner_server_name.getSelectedItem().toString());
	// }
	//
	// @Override
	// public void onNothingSelected(AdapterView<?> arg0) {
	// }
	// });
	// }

	public void renderVSList() {
		spinner_vsName = (Spinner) findViewById(R.id.spinner_pull_vsname);
		List<String> list = new ArrayList<String>();

		for (String vs : vsNameList) {
			list.add(vs);
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

	public void renderRangeType() {
		spinner_rangeType = (Spinner) findViewById(R.id.spinner_range_type);
		List<String> list = new ArrayList<String>();
		for (String vs : RANGE_LIST) {
			list.add(vs);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_rangeType.setAdapter(dataAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Pull").setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			// Intent myIntent = new Intent(this, ActivityHome.class);
			// this.startActivity(myIntent);
			finish();
			break;
		case 0:
			pull(null);
			break;
		}
		return true;
	}

	public void pull(View view) {
		Toast.makeText(this, "Pull", Toast.LENGTH_SHORT).show();
		EditText numLatest = (EditText) findViewById(R.id.editText_numLatest);

		controller.pullLatestData(serverEditText.getText().toString(),
				spinner_vsName.getSelectedItem().toString(), numLatest.getText()
						.toString());
	}

	public void select_server(View view) {
		view.showContextMenu();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		for (String s : SERVER_LIST) {
			menu.add(s);
		}
		
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		// Note how this callback is using the fully-qualified class name
		Toast.makeText(this, "Got click: " + item.toString(), Toast.LENGTH_SHORT)
				.show();
		serverEditText.setText(item.toString());
		
		dialog.show();
		
		controller.loadListVS(item.toString());
		return true;
	}
}
