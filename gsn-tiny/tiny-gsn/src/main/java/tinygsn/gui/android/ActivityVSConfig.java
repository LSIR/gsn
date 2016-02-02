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
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import tinygsn.beans.Subscription;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.utils.ParameterType;
import tinygsn.model.utils.Parameter;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;

public class ActivityVSConfig extends AbstractActivity {
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
	private ArrayList<String> selectedVS = new ArrayList<>();

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

				new AsyncTask<Activity, Void, SettingPanel>() {
					@Override
					protected SettingPanel doInBackground(Activity... params) {

						vssetting = null;
						try {
							ArrayList<String> vp = new ArrayList<>();
							for (String wrapperName : selectedVS) {
								String[] fields = ((AbstractWrapper) Class.forName(wrapperName).newInstance()).getFieldList();
								for (int i = 0; i < fields.length; i++) {
									vp.add(fields[i]);
								}
							}
							ArrayList<Parameter> param = ((AbstractVirtualSensor) Class.forName(AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[pos]).newInstance()).getParameters(vp);
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
				// Get wrapper name of deleted stream source
				String wrapperName = panel.wrapper.getSelectedItem().toString();
				wrapperName = wrapperList.getProperty(wrapperName);
				int index = selectedVS.indexOf(wrapperName);
				//Remove name of the selected list
				selectedVS.remove(index);
				//Update the "field" list in the VS
				updateVSSpinnerParameter("field");
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
		SqliteStorageManager storage = new SqliteStorageManager();
		for (Subscription s : storage.getSubscribeList()){
			try {
				list.add("remote: Subscription "+s.getId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		panel.wrapper.setAdapter(dataAdapter);
		inrow.addView(panel.wrapper);
		layout.addView(inrow);
		final LastVSSelected lastVSSelected = new LastVSSelected();

		// used only to keep somewhere the previous value of a spinner
		// to remove the corresponding fields
		panel.wrapper.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (panel.wrapper != null) {
					lastVSSelected.setLastVSSelected(wrapperList.getProperty(panel.wrapper.getSelectedItem().toString()));
				}
				return false;
			}
		});
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
							} else if (wrapperName.startsWith("remote: ")) {
								wrapperName = "tinygsn.model.wrappers.RemoteWrapper";
							} else {
								wrapperName = wrapperList.getProperty(wrapperName);
							}
							ArrayList<Parameter> param = ((AbstractWrapper) Class.forName(wrapperName).newInstance()).getParameters();
							panel.settings = new SettingPanel("wrapper", param);
							selectedVS.remove(lastVSSelected.getLastVSSelected());
							selectedVS.add(wrapperName);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return panel.settings;
					}

					@Override
					protected void onPostExecute(SettingPanel result) {
						if (result != null) {
							settingLayout.addView(panel.settings.getPanel());
							updateVSSpinnerParameter("field");
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

	/**
	 * Update the spinner list of one field of a Virtual Sensor
	 *
	 * @param paramName
	 */
	private void updateVSSpinnerParameter(String paramName) {
		try {
			for (Parameter param : vssetting.params) {
				if (param.getmName().equals(paramName)) {
					ArrayList<String> vp = new ArrayList<>();
					for (String wrapperName : selectedVS) {
						String[] fields = ((AbstractWrapper) Class.forName(wrapperName).newInstance()).getFieldList();
						for (int i = 0; i < fields.length; i++) {
							if (!vp.contains(fields[i])) {
								vp.add(fields[i]);
							}
						}
					}
					param.setmParameters(vp);
					table_vsensor_config.removeAllViews();
					table_vsensor_config.addView(vssetting.getPanel());
					return;
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

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
					} else if (vsName.matches("(.*)[[\\s*{}\\[\\]#~@'`!\"£$€%^&*()_\\-+=\\/\\?.,<>|\\\\¬|:;]](.*)")) {
						Toast.makeText(this, "VS Name cannot have special characters!",
							Toast.LENGTH_SHORT).show();
					} else if (storage.vsExists("vs_" + vsName) == true) {
						Toast.makeText(this, "VS Name already exists, please choose a new one!",
							Toast.LENGTH_SHORT).show();
					} else if (selectedVS.isEmpty()) {
						Toast.makeText(this, "You must choose at least on wrapper for your VS",
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
            } else if (wrapperName.startsWith("remote: ")) {
                wrapperName = "tinygsn.model.wrappers.RemoteWrapper?" + wrapperName.substring(21);
			} else {
				wrapperName = wrapperList.getProperty(wrapperName);
			}
			if (validate()) {
				storage.executeInsert(
					"sourcesList",
					new ArrayList<>(Arrays.asList("vsname",
						"sswindowsize", "ssstep", "sstimebased", "ssaggregator",
						"wrappername")),
					new ArrayList<>(Arrays.asList(vsname, windowsize.getText().toString(), stepsize
						.getText().toString(), timebased.isChecked() + "", aggregator.getSelectedItemPosition()
						+ "", wrapperName)));
				// As Wrappers are singleton, remove first old settings for this wrapper
				for (Parameter param : settings.params) {
					storage.deleteSetting("wrapper" + ":" + wrapperName + ":" + param.getmName());
				}
				settings.saveTo(wrapperName, storage);
			}
			return wrapperName;

		}
	}

	private class LastVSSelected { // use to keep the value of wrapper in VS to remove corresponding fields
		String lastVSSelected = "";

		public void setLastVSSelected(String s) {
			this.lastVSSelected = s;
		}

		public String getLastVSSelected() {
			return this.lastVSSelected;
		}
	}

	private class SettingPanel { //key-value parameters (for VS and wrappers)

		private String prefix;
		private ArrayList<Parameter> params;
		private TextView[] values;
		private Spinner[] spinners;

		SettingPanel(String prefix, ArrayList<Parameter> params) {
			this.prefix = prefix;
			this.params = params;
		}

		public TableLayout getPanel() {

			TextWatcher textWatcher = new TextWatcher() {
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
					} catch (NumberFormatException e) {
						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ActivityVSConfig.this);
						alertDialogBuilder.setTitle("Please Fill in all fields!");
					}
				}
			};

			TableLayout layout = new TableLayout(ActivityVSConfig.this);
			layout.setColumnStretchable(1, true);
			TableRow.LayoutParams p = new TableRow.LayoutParams();
			p.span = 2;
			p.width = TableRow.LayoutParams.MATCH_PARENT;
			layout.setLayoutParams(p);

			values = new TextView[params.size()];
			spinners = new Spinner[params.size()];

			int indexSpinner = 0;
			int indexTextView = 0;
			for (int i = 0; i < params.size(); i++) {
				TableRow inrow = new TableRow(ActivityVSConfig.this);
				TextView label = new TextView(ActivityVSConfig.this);
				label.setText(params.get(i).getmName() + ": ");
				label.setTextColor(Color.rgb(0, 0, 0));
				inrow.addView(label);

				switch (params.get(i).getmType()) {
					case EDITBOX:
						values[indexTextView] = new EditText(ActivityVSConfig.this);
						break;
					case EDITBOX_NUMBER:
						values[indexTextView] = new EditText(ActivityVSConfig.this);
						values[indexTextView].setInputType(InputType.TYPE_CLASS_NUMBER);
						break;
					case EDITBOX_PHONE:
						values[indexTextView] = new EditText(ActivityVSConfig.this);
						values[indexTextView].setInputType(InputType.TYPE_CLASS_PHONE);
						break;
					case CHECKBOX:
						values[indexTextView] = new CheckBox(ActivityVSConfig.this);
						break;
					case SPINNER:
						spinners[indexSpinner] = new Spinner(ActivityVSConfig.this);
						List<String> list = params.get(i).getmParameters();
						ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context,
							tinygsn.gui.android.R.layout.spinner_item, list);
						dataAdapter
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						spinners[indexSpinner].setAdapter(dataAdapter);
						inrow.addView(spinners[indexSpinner]);
						layout.addView(inrow);
						indexSpinner++;
						break;
					default:
						values[indexTextView] = new EditText(ActivityVSConfig.this);
						break;
				}

				if (params.get(i).getmType() != ParameterType.SPINNER) {
					values[indexTextView].setTextSize(TEXT_SIZE + 5);
					values[indexTextView].setText(params.get(i).getmDefaultParameter());
					values[indexTextView].setTextColor(Color.rgb(0, 0, 0));
					inrow.addView(values[indexTextView]);
					values[indexTextView].addTextChangedListener(textWatcher);
					indexTextView++;
					layout.addView(inrow);
				}
			}
			return layout;
		}

		public void saveTo(String module, SqliteStorageManager storage) {
			int indexSpinner = 0;
			int indexTextView = 0;
			for (int i = 0; i < params.size(); i++) {
				String value;
				if (params.get(i).getmType() == ParameterType.SPINNER) {
					value = spinners[indexSpinner].getSelectedItem().toString();
					indexSpinner++;
				} else if (params.get(i).getmType() == ParameterType.CHECKBOX) {
					boolean boolValue = ((CheckBox) values[indexTextView]).isChecked();
					if (boolValue) {
						value = "true";
					} else {
						value = "false";
					}
					indexTextView++;
				} else {
					value = values[indexTextView].getText().toString();
					indexTextView++;
				}
				storage.setSetting(prefix + ":" + module + ":" + params.get(i).getmName(), value);
			}
		}
	}
}
