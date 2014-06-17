package tinygsn.gui.android;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import tinygsn.beans.DataField;
import tinygsn.beans.StreamSource;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.vsensor.NotificationVirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ActivityVSConfig extends SherlockActivity {
	private static final String TAG = "AndroidVSConfigSetting";
	static int TEXT_SIZE = 10;
	private Context context = this;
	private Spinner spinnerVSType, spinnerAggregate, spinnerWrapper, field,
			condition, action;
	private EditText editText_vsName, editText_ssWindowSize, editText_ssStep,
			editText_ssSamplingRate, editText_value, editText_contact;
	private TableLayout table_notify_config = null;
	private CheckBox saveToDB;

	private SqliteStorageManager storage = null;
	private Properties wrapperList;

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
		editText_ssWindowSize = (EditText) findViewById(R.id.editText_ssWindowSize);
		editText_ssStep = (EditText) findViewById(R.id.editText_ssStep);
		editText_ssSamplingRate = (EditText) findViewById(R.id.editText_ssSamplingRate);
		table_notify_config = (TableLayout) findViewById(R.id.table_notify_config);

		wrapperList = AbstractWrapper.getWrapperList(this);

		loadVSType();
		loadAggregatorType();
		loadWrapperList();

		storage = new SqliteStorageManager(this);
	}

	public void loadVSType() {
		spinnerVSType = (Spinner) findViewById(R.id.spinner_vsType);
		List<String> list = new ArrayList<String>();

		for (String s : AbstractVirtualSensor.VIRTUAL_SENSOR_LIST) {
			list.add(s);
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerVSType.setAdapter(dataAdapter);

		spinnerVSType.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				Toast.makeText(
						parent.getContext(),
						"The virtual sensor \"" + parent.getItemAtPosition(pos).toString()
								+ "\" is selected.", Toast.LENGTH_SHORT).show();
				if (pos == 1) {
					addViewNotifyConfig();
				}
				else
					table_notify_config.removeAllViews();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Toast.makeText(context, "Please select a virtual sensor",
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void loadAggregatorType() {
		spinnerAggregate = (Spinner) findViewById(R.id.spinner_ssAggregate);
		List<String> list = new ArrayList<String>();

		for (String s : StreamSource.AGGREGATOR) {
			list.add(s);
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAggregate.setAdapter(dataAdapter);

		spinnerAggregate.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				Toast.makeText(
						parent.getContext(),
						"The aggregate funtion \""
								+ parent.getItemAtPosition(pos).toString() + "\" is selected.",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	public void loadWrapperList() {
		spinnerWrapper = (Spinner) findViewById(R.id.spinner_wType);
		List<String> list = new ArrayList<String>();

		for (String s : wrapperList.stringPropertyNames()) {
			list.add(s);
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerWrapper.setAdapter(dataAdapter);

		spinnerWrapper.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				Toast.makeText(
						parent.getContext(),
						"The wrapper \"" + parent.getItemAtPosition(pos).toString()
								+ "\" is selected.", Toast.LENGTH_SHORT).show();
				if (spinnerVSType.getSelectedItemPosition() == 1) {
					addViewNotifyConfig();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
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
		String wrapperName = wrapperList.getProperty(spinnerWrapper
				.getSelectedItem().toString());

		try {
			AbstractWrapper w = (AbstractWrapper) Class.forName(wrapperName)
					.newInstance();
			for (String s : w.getFieldList()) {
				list.add(s);
			}
		}
		catch (InstantiationException e1) {
			e1.printStackTrace();
		}
		catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}
		catch (ClassNotFoundException e1) {
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
		editText_contact.setText("+41786302385");
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

		// saveToDB.setOnClickListener(new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		//
		// }
		// });

		table_notify_config.addView(row);
		// TableRow.LayoutParams params = new TableRow.LayoutParams();
		// params.span = 2;

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	public void saveVS(View view) {
		String vsName = editText_vsName.getText().toString();

		if (vsName.equals("")) {
			Toast.makeText(this, "Please input VS Name", Toast.LENGTH_SHORT).show();
			return;
		}
		if (editText_ssWindowSize.getText().toString().equals("")) {
			Toast.makeText(this, "Please input Window Size", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		if (editText_ssStep.getText().toString().equals("")) {
			Toast.makeText(this, "Please input Step", Toast.LENGTH_SHORT).show();
			return;
		}
		if (editText_ssSamplingRate.getText().toString().equals("")) {
			Toast.makeText(this, "Please input Sampling Rate", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		if (storage.vsExists("vs_" + vsName) == true) {
			Toast.makeText(this, "VS Name already exists, please choose a new one!",
					Toast.LENGTH_SHORT).show();
			return;
		}

		String notify_field = "", notify_condition = "", notify_value = "", notify_action = "", notify_contact = "", save_to_db = "";

		String vsType = "1";
		if (spinnerVSType.getSelectedItemPosition() == 1) {
			vsType = "2";

			notify_field = field.getSelectedItem().toString();
			notify_condition = condition.getSelectedItem().toString();
			notify_value = editText_value.getText().toString();
			notify_action = action.getSelectedItem().toString();
			notify_contact = editText_contact.getText().toString();
			save_to_db = saveToDB.isChecked() + "";
		}

		// Log.v(TAG, "save_to_db is " + save_to_db);

		String wrapperName = wrapperList.getProperty(spinnerWrapper
				.getSelectedItem().toString());

		try {
			storage
					.executeInsert(
							"vsList",
							new ArrayList<String>(Arrays.asList("running", "vsname",
									"vstype", "sswindowsize", "ssstep", "sssamplingrate",
									"ssaggregator", "wrappername", "notify_field",
									"notify_condition", "notify_value", "notify_action",
									"notify_contact", "save_to_db")),
							new ArrayList<String>(Arrays.asList("1", vsName, vsType,
									editText_ssWindowSize.getText().toString(), editText_ssStep
											.getText().toString(), editText_ssSamplingRate.getText()
											.toString(), spinnerAggregate.getSelectedItemPosition()
											+ "", wrapperName, notify_field, notify_condition,
									notify_value, notify_action, notify_contact, save_to_db)));

			AbstractWrapper w;
			try {
				w = (AbstractWrapper) Class.forName(wrapperName).newInstance();
				DataField[] outputStructure = w.getOutputStructure();
				storage.createTable("vs_" + vsName, outputStructure);
			}
			catch (InstantiationException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			Toast.makeText(this, "Save new virtual sensor successfully!",
					Toast.LENGTH_SHORT).show();

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
//			Toast.makeText(this, "Home pressed", Toast.LENGTH_SHORT).show();
//			Intent myIntent = new Intent(this, ActivityListVSNew.class);
//			this.startActivity(myIntent);
			finish();
			
			break;
		case 0:
			if (isEnableSave) {
				saveVS(null);
				isEnableSave = false;
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

}
