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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityVSConfig.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import tinygsn.beans.DataField;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamSource;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.vsensor.NotificationVirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ActivityVSConfig extends SherlockActivity {
	static int TEXT_SIZE = 10;
	private Context context = this;
	private Spinner spinnerVSType, field, condition, action;
	private EditText editText_vsName, editText_value, editText_contact;
	private TableLayout table_notify_config, table_layout;
	private TableRow table_vsensor_config;
	private CheckBox saveToDB;
    private SettingPanel vssetting;
	private SqliteStorageManager storage = null;
	private Properties wrapperList;
	AndroidControllerVS controller = new AndroidControllerVS();
	
	private ArrayList<StreamSourcePanel> pannels = new ArrayList<ActivityVSConfig.StreamSourcePanel>();

	private boolean isEnableSave = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vs_config);

		// This is a workaround for http://b.android.com/15340 from
		// http://stackoverflow.com/a/5852198/132047
		// if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
		// }
		BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(
				R.drawable.bg_striped);
		bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		getSupportActionBar().setBackgroundDrawable(bg);

		BitmapDrawable bgSplit = (BitmapDrawable) getResources().getDrawable(
				R.drawable.bg_striped_split_img);
		bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		getSupportActionBar().setSplitBackgroundDrawable(bgSplit);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		editText_vsName = (EditText) findViewById(R.id.editText_vsName);
		table_notify_config = (TableLayout) findViewById(R.id.table_notify_config);
		table_vsensor_config = (TableRow) findViewById(R.id.table_vsensor_config);
		table_layout = (TableLayout) findViewById(R.id.tableLayout_vs);
		Button button_add = (Button) findViewById(R.id.button_add);
		button_add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	table_layout.addView(addSource());
            }
        });

		wrapperList = AbstractWrapper.getWrapperList(this);
		loadVSType();		
		storage = new SqliteStorageManager();
	}

	public void loadVSType() {
		spinnerVSType = (Spinner) findViewById(R.id.spinner_vsType);
		List<String> list = new ArrayList<String>();

		for (String s : AbstractVirtualSensor.VIRTUAL_SENSOR_LIST) {
			list.add(s);
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerVSType.setAdapter(dataAdapter);

		spinnerVSType.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, final int pos, long id) {
				table_vsensor_config.removeAllViews();
				
				if (pos == 1) addViewNotifyConfig();
				else table_notify_config.removeAllViews();
				
				new AsyncTask<Activity, Void, SettingPanel>(){
					@Override
					protected SettingPanel doInBackground(Activity... params) {

						vssetting = null;
						try {
							String[] param = ((AbstractVirtualSensor) Class.forName(AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[pos]).newInstance()).getParameters();
							vssetting = new SettingPanel("vsensor", param);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return vssetting;
					}
					
					@Override
					protected void onPostExecute(SettingPanel result) {
						if(result != null){
							table_vsensor_config.addView(vssetting.getPanel());
						}
					}
					
				}.execute((Activity)null);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Toast.makeText(context, "Please select a virtual sensor",
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	private TableRow addSource(){
		
		final StreamSourcePanel panel = new StreamSourcePanel();
		final TableRow row = new TableRow(this);
		final TableLayout settingLayout = new TableLayout(this);
		TableLayout layout = new TableLayout(this);
		TableRow.LayoutParams p = new TableRow.LayoutParams();
		p.span = 2;
		p.width = TableRow.LayoutParams.MATCH_PARENT;
		layout.setColumnStretchable(1, true);
		layout.setLayoutParams(p);
		settingLayout.setLayoutParams(p);
		settingLayout.setColumnStretchable(1, true);
		row.addView(layout);
		TableRow inrow = new TableRow(this);
		layout.addView(inrow);
		LinearLayout separator = new LinearLayout(this);
		separator.setBackgroundColor(Color.rgb(150, 150, 150));
		p = new TableRow.LayoutParams();
		p.span = 2;
		separator.setLayoutParams(p);
		inrow.addView(separator);
		
		Button b = new Button(this);
		b.setText("-");
		separator.addView(b);
		b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	pannels.remove(panel);
            	table_layout.removeView(row);
            }
        });
		
		//window size
		inrow = new TableRow(this);
		TextView label = new TextView(this);
		label.setText("Window size: ");
		label.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(label);
		
		panel.windowsize = new EditText(this);
		panel.windowsize.setText("5");
		panel.windowsize.setTextSize(TEXT_SIZE + 5);
		panel.windowsize.setInputType(InputType.TYPE_CLASS_NUMBER);
		panel.windowsize.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(panel.windowsize);
		layout.addView(inrow);
		
		//step size
		inrow = new TableRow(this);
		label = new TextView(this);
		label.setText("Step size: ");
		label.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(label);
		
		panel.stepsize = new EditText(this);
		panel.stepsize.setText("1");
		panel.stepsize.setTextSize(TEXT_SIZE + 5);
		panel.stepsize.setInputType(InputType.TYPE_CLASS_NUMBER);
		panel.stepsize.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(panel.stepsize);
		layout.addView(inrow);

		//time based
		inrow = new TableRow(this);
		label = new TextView(this);
		label.setText("Time based? ");
		label.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(label);
		
		panel.timebased = new CheckBox(this);
		panel.timebased.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(panel.timebased);
		layout.addView(inrow);
		
		//aggregator
		inrow = new TableRow(this);
		label = new TextView(this);
		label.setText("Aggregation: ");
		label.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(label);
		
		panel.aggregator = new Spinner(this);
		List<String> list = new ArrayList<String>();
        for (String s : StreamSource.AGGREGATOR){
				list.add(s);
        }
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		panel.aggregator.setAdapter(dataAdapter);
		inrow.addView(panel.aggregator);
		layout.addView(inrow);
		
		//wrapper
		inrow = new TableRow(this);
		label = new TextView(this);
		label.setText("Wrapper: ");
		label.setTextColor(Color.rgb(0, 0, 0));
		inrow.addView(label);
		
		panel.wrapper = new Spinner(this);
		list = new ArrayList<String>();
        for (String s : wrapperList.stringPropertyNames()){
				list.add(s);
        }
        for (String s : storage.getListofVSName()){
        	list.add("local: "+s);
        }
		dataAdapter = new ArrayAdapter<String>(this,R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		panel.wrapper.setAdapter(dataAdapter);
		inrow.addView(panel.wrapper);
		layout.addView(inrow);
		
		panel.wrapper.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
							
				settingLayout.removeAllViews();
				panel.settings = null;
				new AsyncTask<Activity, Void, SettingPanel>(){
					@Override
					protected SettingPanel doInBackground(Activity... params) {
						try {
							String wrapperName = panel.wrapper.getSelectedItem().toString();
							wrapperName  = wrapperList.getProperty(wrapperName);
							String[] param = ((AbstractWrapper) Class.forName(wrapperName).newInstance()).getParameters();
							panel.settings = new SettingPanel("wrapper", param);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return panel.settings;
					}
					@Override
					protected void onPostExecute(SettingPanel result) {
						if(result != null){
							settingLayout.addView(panel.settings.getPanel());
						}
					}
				}.execute((Activity)null);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Toast.makeText(context, "Please select a virtual sensor",
						Toast.LENGTH_SHORT).show();
			}
		});
		
		//setting container
		inrow = new TableRow(this);
		inrow.addView(settingLayout);
		layout.addView(inrow);
		
		pannels.add(panel);
		return row;
	}

	public void addViewNotifyConfig() {
		table_notify_config.removeAllViews();

		TableRow row = new TableRow(this);

		// Row Field
		TextView txt = new TextView(this);
		txt.setText("Field");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		field = new Spinner(this);
		List<String> list = new ArrayList<String>();
		String wrapperName = wrapperList.getProperty("gps"); //TODO dynamic

		try {
			AbstractWrapper w = (AbstractWrapper) StaticData.getWrapperByName(wrapperName);
			for (String s : w.getFieldList()) {
				list.add(s);
			}
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		field.setAdapter(dataAdapter);

		row.addView(field);
		table_notify_config.addView(row);

		// Row condition
		row = new TableRow(this);

		txt = new TextView(this);
		txt.setText("Sampling Rate");
		txt.setText("Condition    ");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		condition = new Spinner(this);
		List<String> list_condition = new ArrayList<String>();

		for (String s : NotificationVirtualSensor.CONDITIONS) {
			list_condition.add(s);
		}
		ArrayAdapter<String> dataAdapter_condition = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list_condition);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		condition.setAdapter(dataAdapter_condition);
		row.addView(condition);

		table_notify_config.addView(row);

		// Row value
		row = new TableRow(this);

		txt = new TextView(this);
		txt.setText("Value");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		editText_value = new EditText(this);
		editText_value.setText("10");
		editText_value.setTextSize(TEXT_SIZE + 5);
		editText_value.setInputType(InputType.TYPE_CLASS_NUMBER);
		// editText_value.requestFocus();
		editText_value.setTextColor(Color.parseColor("#000000"));
		row.addView(editText_value);

		editText_value.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					// numLatest =
					// Integer.parseInt(editText_numLatest.getText().toString());
					// loadLatestData();
				}
				catch (NumberFormatException e) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							context);
					alertDialogBuilder.setTitle("Please input a number!");
				}
			}
		});

		table_notify_config.addView(row);

		// Row action
		row = new TableRow(this);

		txt = new TextView(this);
		txt.setText("Action");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		action = new Spinner(this);
		List<String> list_action = new ArrayList<String>();

		for (String s : NotificationVirtualSensor.ACTIONS) {
			list_action.add(s);
		}
		ArrayAdapter<String> dataAdapter_action = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list_action);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		action.setAdapter(dataAdapter_action);
		row.addView(action);

		table_notify_config.addView(row);

		// Row contact
		row = new TableRow(this);

		txt = new TextView(this);
		txt.setText("Contact");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		editText_contact = new EditText(this);
		editText_contact.setText("+41798765432");
		editText_contact.setTextSize(TEXT_SIZE + 5);
		// editText_contact.setInputType(InputType.TYPE_CLASS_NUMBER);
		// editText_contact.requestFocus();
		editText_contact.setTextColor(Color.parseColor("#000000"));
		row.addView(editText_contact);

		editText_contact.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					// numLatest =
					// Integer.parseInt(editText_numLatest.getText().toString());
					// loadLatestData();
				}
				catch (NumberFormatException e) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							context);
					alertDialogBuilder.setTitle("Please input a phone number!");
				}
			}
		});

		table_notify_config.addView(row);

		// Row Save to DB
		row = new TableRow(this);

		txt = new TextView(this);
		txt.setText("Save to Database?");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		saveToDB = new CheckBox(this);
		saveToDB.setTextColor(Color.parseColor("#000000"));
		row.addView(saveToDB);

		
		table_notify_config.addView(row);
		// TableRow.LayoutParams params = new TableRow.LayoutParams();
		// params.span = 2;

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	public void saveVS() {
		String vsName = editText_vsName.getText().toString();
		String notify_field = "", notify_condition = "", notify_value = "", notify_action = "", notify_contact = "", save_to_db = "";

		int vsType = spinnerVSType.getSelectedItemPosition();
		if (vsType == 1) {
			notify_field = field.getSelectedItem().toString();
			notify_condition = condition.getSelectedItem().toString();
			notify_value = editText_value.getText().toString();
			notify_action = action.getSelectedItem().toString();
			notify_contact = editText_contact.getText().toString();
			save_to_db = saveToDB.isChecked() + "";
		}

		try {
			storage.executeInsert(
							"vsList",
							new ArrayList<String>(Arrays.asList("running", "vsname",
									"vstype", "notify_field", "notify_condition", "notify_value", "notify_action",
									"notify_contact", "save_to_db")),
							new ArrayList<String>(Arrays.asList("1", vsName, ""+vsType, notify_field, notify_condition,
									notify_value, notify_action, notify_contact, save_to_db)));
			
			vssetting.saveTo(vsName, storage);
			
			String wrapperName=""; 
			for(StreamSourcePanel p:pannels){
				wrapperName = p.saveTo(vsName,storage); // TODO compute actual output structure !!!
			}
			
			AbstractWrapper w;
			try {
				w = StaticData.getWrapperByName(wrapperName);
				DataField[] outputStructure = w.getOutputStructure();
				AbstractVirtualSensor vs = (AbstractVirtualSensor) Class.forName(AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[vsType]).newInstance();
				outputStructure = vs.getOutputStructure(outputStructure);
				storage.executeCreateTable("vs_" + vsName, outputStructure,true);
				StaticData.saveName(vsName, wrapperName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Save")
//				.setIcon(R.drawable.ic_menu_save)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			
			break;
		case 0:
			if (isEnableSave) {
				String vsName = editText_vsName.getText().toString();
				if (vsName.equals("")) {
					Toast.makeText(this, "Please input VS Name", Toast.LENGTH_SHORT).show();
				}
				else if (storage.vsExists("vs_" + vsName) == true) {
					Toast.makeText(this, "VS Name already exists, please choose a new one!",
							Toast.LENGTH_SHORT).show();
				}
				else {
					new AsyncTask<Activity, Void, Void>(){
						@Override
						protected Void doInBackground(Activity... params) {
							saveVS();
							isEnableSave = false;
							String vsName = editText_vsName.getText().toString();
							controller.startStopVS(vsName, true);
							return null;
						}
						@Override
						protected void onPostExecute(Void result) {
							finish();
						}
					}.execute(this);
				}
				
			}
			else
				Toast.makeText(this, "There is nothing changed to save!",
						Toast.LENGTH_SHORT).show();
			break;
		}

		// Toast.makeText(this, "itemId " + itemId + " pressed", Toast.LENGTH_SHORT)
		// .show();

		return true;
	}

	void enableSave(boolean isEnabled) {
		Button saveButton = (Button) findViewById(R.id.btnSaveVS);
		saveButton.setEnabled(isEnabled);
	}
	
	private class StreamSourcePanel{
		public EditText windowsize, stepsize;
		public Spinner aggregator, wrapper;
		public CheckBox timebased;
		public SettingPanel settings;
		
		public boolean validate(){
		if (windowsize.getText().toString().equals("")) {
			Toast.makeText(ActivityVSConfig.this, "Please input Window Size", Toast.LENGTH_SHORT)
					.show();
			return false;
		}
		if (stepsize.getText().toString().equals("")) {
			Toast.makeText(ActivityVSConfig.this, "Please input Step", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
		}
		
		public String saveTo(String vsname, SqliteStorageManager storage) throws SQLException {
			String wrapperName = wrapper.getSelectedItem().toString();
			if(wrapperName.startsWith("local: ")){
				wrapperName = "tinygsn.model.wrappers.LocalWrapper?"+wrapperName.substring(7);
			}else{
				wrapperName  = wrapperList.getProperty(wrapperName);
			}
			if (validate()){
			    storage.executeInsert(
					"sourcesList",
					new ArrayList<String>(Arrays.asList("vsname",
							"sswindowsize", "ssstep", "sstimebased", "ssaggregator",
							"wrappername")),
					new ArrayList<String>(Arrays.asList(vsname, windowsize.getText().toString(), stepsize
							.getText().toString(),timebased.isChecked()+"", aggregator.getSelectedItemPosition()
							+ "", wrapperName)));
			    settings.saveTo(wrapperName, storage);
			
			}
			return wrapperName;
			
		}
	}
	
	private class SettingPanel{ //key-value parameters (for VS and wrappers)
		
		private String prefix;
		private String[] params; 
		private EditText[] values;
		
		SettingPanel(String prefix, String[] params){
			this.prefix = prefix;
			this.params = params;
		}
		
		public TableLayout getPanel(){
			
			TableLayout layout = new TableLayout(ActivityVSConfig.this);
			layout.setColumnStretchable(1, true);
			TableRow.LayoutParams p = new TableRow.LayoutParams();
			p.span = 2;
			p.width = TableRow.LayoutParams.MATCH_PARENT;
			layout.setLayoutParams(p);

			values = new EditText[params.length];

			for(int i=0; i<params.length;i++){
				TableRow inrow = new TableRow(ActivityVSConfig.this);
				TextView label = new TextView(ActivityVSConfig.this);
				label.setText(params[i]+": ");
				label.setTextColor(Color.rgb(0, 0, 0));
				inrow.addView(label);
				
				values[i] = new EditText(ActivityVSConfig.this);
				values[i].setTextSize(TEXT_SIZE + 5);
				values[i].setTextColor(Color.rgb(0, 0, 0));
				inrow.addView(values[i]);
				layout.addView(inrow);
			}
			return layout;
		}
		
		public void saveTo(String module, SqliteStorageManager storage) {
			for(int i=0; i<params.length;i++){
				storage.setSetting(prefix+":"+module+":"+params[i], values[i].getText().toString());
			}
			
		}
		
		
	}


}
