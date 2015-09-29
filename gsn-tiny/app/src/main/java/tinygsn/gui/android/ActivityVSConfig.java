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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityVSConfig.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import tinygsn.beans.DataField;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamSource;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;

public class ActivityVSConfig extends Activity {
	static int TEXT_SIZE = 10;
	private Context context = this;
	private Spinner spinnerVSType;
	private EditText editText_vsName;
	private TableLayout table_notify_config, table_layout;
	private TableRow table_vsensor_config;
	private SettingPanel vssetting;
	private SqliteStorageManager storage = null;
	private Properties wrapperList;
	AndroidControllerVS controller = new AndroidControllerVS();

	private ArrayList<StreamSourcePanel> pannels = new ArrayList<>();

	private boolean isEnableSave = true;
	private String editingVS = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vs_config);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			editingVS = extras.getString("vsname");
			loadEditingValues();
		}

		getActionBar().setDisplayHomeAsUpEnabled(true);

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
		storage = new SqliteStorageManager();
		loadVSType();
	}

	public void loadEditingValues() {
		if (editingVS != null) {
			new AsyncTask<Activity, Void, AbstractVirtualSensor>() {
				@Override
				protected AbstractVirtualSensor doInBackground(Activity... params) {
					return storage.getVSByName(editingVS);
				}

				@Override
				protected void onPostExecute(AbstractVirtualSensor result) {
					if (result != null) {
						result.getVirtualSensorConfiguration().getName();
						//TODO load values inside fields and create the input sources
						//some of them must be set readonly !!!
					}
				}
			}.execute((Activity) null);
		}
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

				try {
					table_notify_config.removeAllViews();
					((AbstractVirtualSensor) Class.forName(AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[pos]).newInstance()).getRowParameters(table_notify_config, context);
				} catch (Exception e) {
					e.printStackTrace();
				}

				new AsyncTask<Activity, Void, SettingPanel>() {
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
						if (result != null) {
							table_vsensor_config.addView(vssetting.getPanel());
						}
					}

				}.execute((Activity) null);

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Toast.makeText(context, "Please select a virtual sensor",
						              Toast.LENGTH_SHORT).show();
			}
		});
	}

	private TableRow addSource() {

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
		List<String> list = new ArrayList<>();
		for (String s : StreamSource.AGGREGATOR) {
			list.add(s);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
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
		list = new ArrayList<>();
		for (String s : wrapperList.stringPropertyNames()) {
			list.add(s);
		}
		Collections.sort(list);
		for (String s : storage.getListofVSName()) {
			list.add("local: " + s);
		}
		dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		panel.wrapper.setAdapter(dataAdapter);
		inrow.addView(panel.wrapper);
		layout.addView(inrow);

		panel.wrapper.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

				settingLayout.removeAllViews();
				panel.settings = null;
				new AsyncTask<Activity, Void, SettingPanel>() {
					@Override
					protected SettingPanel doInBackground(Activity... params) {
						try {
							String wrapperName = panel.wrapper.getSelectedItem().toString();
							if (wrapperName.startsWith("local: ")) {
								wrapperName = "tinygsn.model.wrappers.LocalWrapper";
							} else {
								wrapperName = wrapperList.getProperty(wrapperName);
							}
							String[] param = ((AbstractWrapper) Class.forName(wrapperName).newInstance()).getParameters();
							panel.settings = new SettingPanel("wrapper", param);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return panel.settings;
					}

					@Override
					protected void onPostExecute(SettingPanel result) {
						if (result != null) {
							settingLayout.addView(panel.settings.getPanel());
						}
					}
				}.execute((Activity) null);

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

	@Override
	protected void onPause() {
		super.onPause();
	}

	//FIXME : think about how to create the row for db
	public void saveVS() {
		String vsName = editText_vsName.getText().toString();

		int vsType = spinnerVSType.getSelectedItemPosition();

		try {
			storage.executeInsert(
					                     "vsList",
					                     new ArrayList<String>(Arrays.asList("running", "vsname",
							                                                        "vstype")),
					                     new ArrayList<String>(Arrays.asList("1", vsName, "" + vsType)));

			vssetting.saveTo(vsName, storage);

			String wrapperName = "";
			for (StreamSourcePanel p : pannels) {
				wrapperName = p.saveTo(vsName, storage); // TODO compute actual output structure !!!
			}

			//take the structure from the last wrapper !!
			AbstractWrapper w;
			try {
				w = StaticData.getWrapperByName(wrapperName);
				DataField[] outputStructure = w.getOutputStructure();
				AbstractVirtualSensor vs = (AbstractVirtualSensor) Class.forName(AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[vsType]).newInstance();
				outputStructure = vs.getOutputStructure(outputStructure);
				storage.executeCreateTable("vs_" + vsName, outputStructure, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
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
					} else if (storage.vsExists("vs_" + vsName) == true) {
						Toast.makeText(this, "VS Name already exists, please choose a new one!",
								              Toast.LENGTH_SHORT).show();
					} else {
						new AsyncTask<Activity, Void, Void>() {
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

				} else
					Toast.makeText(this, "There is nothing changed to save!",
							              Toast.LENGTH_SHORT).show();
				break;
		}

		return true;
	}

	void enableSave(boolean isEnabled) {
		Button saveButton = (Button) findViewById(R.id.btnSaveVS);
		saveButton.setEnabled(isEnabled);
	}

	private class StreamSourcePanel {
		public EditText windowsize, stepsize;
		public Spinner aggregator, wrapper;
		public CheckBox timebased;
		public SettingPanel settings;

		public boolean validate() {
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
			if (wrapperName.startsWith("local: ")) {
				wrapperName = "tinygsn.model.wrappers.LocalWrapper?" + wrapperName.substring(7);
			} else {
				wrapperName = wrapperList.getProperty(wrapperName);
			}
			if (validate()) {
				storage.executeInsert(
						                     "sourcesList",
						                     new ArrayList<String>(Arrays.asList("vsname",
								                                                        "sswindowsize", "ssstep", "sstimebased", "ssaggregator",
								                                                        "wrappername")),
						                     new ArrayList<String>(Arrays.asList(vsname, windowsize.getText().toString(), stepsize
								                                                                                                  .getText().toString(), timebased.isChecked() + "", aggregator.getSelectedItemPosition()
										                                                                                                                                                     + "", wrapperName)));
				settings.saveTo(wrapperName, storage);

			}
			return wrapperName;

		}
	}

	private class SettingPanel { //key-value parameters (for VS and wrappers)

		private String prefix;
		private String[] params;
		private EditText[] values;

		SettingPanel(String prefix, String[] params) {
			this.prefix = prefix;
			this.params = params;
		}

		public TableLayout getPanel() {

			TableLayout layout = new TableLayout(ActivityVSConfig.this);
			layout.setColumnStretchable(1, true);
			TableRow.LayoutParams p = new TableRow.LayoutParams();
			p.span = 2;
			p.width = TableRow.LayoutParams.MATCH_PARENT;
			layout.setLayoutParams(p);

			values = new EditText[params.length];

			for (int i = 0; i < params.length; i++) {
				TableRow inrow = new TableRow(ActivityVSConfig.this);
				TextView label = new TextView(ActivityVSConfig.this);
				label.setText(params[i] + ": ");
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
			for (int i = 0; i < params.length; i++) {
				storage.setSetting(prefix + ":" + module + ":" + params[i], values[i].getText().toString());
			}

		}


	}


}
