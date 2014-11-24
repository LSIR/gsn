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
* File: gsn-tiny/src/tinygsn/gui/android/ActivityViewDataNew.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerViewDataNew;
import tinygsn.gui.android.chart.AbstractDemoChart;
import tinygsn.gui.android.chart.SensorValuesChart;
import tinygsn.gui.android.utils.VSListAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

@SuppressLint({ "HandlerLeak", "SimpleDateFormat" })
@SuppressWarnings("unchecked")
public class ActivityViewDataNew extends SherlockFragmentActivity {
	static int TEXT_SIZE = 10;

	private Context context = this;
	// private Button btnLoad = null;
	private AndroidControllerViewDataNew controller;
	private Handler handlerData, handlerVS, handlerField;
	private TextView lblOutput = null;
	// private EditText numLatestTxt = null;
	private Spinner spinnerVS, spinnerField, spinnerViewMode;
	private TableLayout table_view_mode = null;

	private ArrayList<StreamElement> streamElements = null;
	private ArrayList<String> vsNameList = null;
	private ArrayList<String> fieldList = null;
	private int numLatest = 10;
	private String vsName = null;
	private String vsNameFromExtra = null;

	protected int viewMode = 1;
	protected String fieldName;

	private TextView txtStartDate, txtStartTime, txtEndDate, txtEndTime;

	private Calendar dateAndTime = Calendar.getInstance();
	private Date startTime, endTime;
	private static final String TAG = "ActivityViewData";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_data);
		lblOutput = (TextView) findViewById(R.id.txbViewData);
		lblOutput.setTextSize(TEXT_SIZE);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			vsNameFromExtra = extras.getString(VSListAdapter.EXTRA_VS_NAME);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addHandlers();

		controller.loadListVS();

		loadViewMode();
	}

	private void addHandlers() {
		controller = new AndroidControllerViewDataNew(this);

		handlerVS = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				vsNameList = (ArrayList<String>) msg.obj;
				loadVSList();
			}
		};
		controller.setHandlerVS(handlerVS);

		handlerField = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				fieldList = (ArrayList<String>) msg.obj;
				loadFieldList();
			}
		};
		controller.setHandlerField(handlerField);

		handlerData = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				streamElements = (ArrayList<StreamElement>) msg.obj;
				if (streamElements.size() == 0)
					outputData("No data!");
				else {
					outputData(getDataOutput());
				}
			}
		};
		controller.setHandlerData(handlerData);
	}

	public void loadVSList() {
		spinnerVS = (Spinner) findViewById(R.id.spinner_vs);
		List<String> list = new ArrayList<String>();
		for (String s : vsNameList) {
			list.add(s);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerVS.setAdapter(dataAdapter);

		int index = 0;
		if (vsNameFromExtra != null) {
			for (String s : vsNameList) {
				if (s.equals(vsNameFromExtra)) {
					spinnerVS.setSelection(index);
				}
				index++;
			}
		}

		spinnerVS.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				vsName = parent.getItemAtPosition(pos).toString();
				Toast.makeText(parent.getContext(),
						"The virtual sensor \"" + vsName + "\" is selected.",
						Toast.LENGTH_SHORT).show();
				controller.loadListFields(parent.getItemAtPosition(pos).toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Toast.makeText(context, "Please select a virtual sensor",
						Toast.LENGTH_SHORT).show();
			}
		});

	}

	protected void loadFieldList() {
		spinnerField = (Spinner) findViewById(R.id.spinner_field);
		List<String> list = new ArrayList<String>();
		for (String s : fieldList) {
			list.add(s);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerField.setAdapter(dataAdapter);

		spinnerField.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				fieldName = parent.getItemAtPosition(pos).toString();
				Toast.makeText(parent.getContext(),
						"The field \"" + fieldName + "\" is selected.", Toast.LENGTH_SHORT)
						.show();
				loadData();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Toast.makeText(context, "Please select a field", Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	protected void loadViewMode() {
		spinnerViewMode = (Spinner) findViewById(R.id.spinner_view_mode);
		List<String> list = new ArrayList<String>();
		list.add("Latest values");
		list.add("Customize time");

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerViewMode.setAdapter(dataAdapter);

		spinnerViewMode.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				// Toast.makeText(
				// parent.getContext(),
				// "The view mode \"" + parent.getItemAtPosition(pos).toString()
				// + "\" is selected.", Toast.LENGTH_SHORT).show();
				if (pos == 0) {
					addTableViewModeLatest();
					viewMode = 1;
					loadData();
				}
				else {
					addTableViewModeCustomize();
					viewMode = 2;
					loadData();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void addTableViewModeLatest() {
		table_view_mode = (TableLayout) findViewById(R.id.table_view_mode);
		// table_view_mode.setLayoutParams(new
		// TableLayout.LayoutParams(LayoutParams.FILL_PARENT,
		// LayoutParams.WRAP_CONTENT));
		table_view_mode.removeAllViews();

		TableRow row = new TableRow(this);

		TextView txt = new TextView(this);
		txt.setText("         View ");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		final EditText editText_numLatest = new EditText(this);
		editText_numLatest.setText("10");
		editText_numLatest.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText_numLatest.requestFocus();
		editText_numLatest.setTextColor(Color.parseColor("#000000"));
		row.addView(editText_numLatest);

		editText_numLatest.addTextChangedListener(new TextWatcher() {
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
					numLatest = Integer.parseInt(editText_numLatest.getText().toString());
					loadLatestData();
				}
				catch (NumberFormatException e) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							context);
					alertDialogBuilder.setTitle("Please input a number!");
				}
			}
		});

		txt = new TextView(this);
		txt.setText(" latest values");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		txt = new TextView(this);
		txt.setText("            ");
		row.addView(txt);
		table_view_mode.addView(row);

		row = new TableRow(this);
		Button detailBtn = new Button(this);
		detailBtn.setText("Detail");

		// detailBtn.setBackground(getResources().getDrawable(R.drawable.info));
		// detailBtn.setWidth(200);
		detailBtn.setTextSize(TEXT_SIZE + 2);
		detailBtn.setTextColor(Color.parseColor("#000000"));
		detailBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialogDetail();
			}
		});

		Button plotDataBtn = new Button(this);
		plotDataBtn.setText("Plot data");
		plotDataBtn.setTextSize(TEXT_SIZE + 2);
		plotDataBtn.setTextColor(Color.parseColor("#000000"));
		// plotDataBtn.setBackground(getResources().getDrawable(R.drawable.chart));

		// LinearLayout.LayoutParams params = plotDataBtn.getLayoutParams();
		// params.width = 50;
		// plotDataBtn.setLayoutParams(params);

		plotDataBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewChart();
			}
		});

		TableRow.LayoutParams rowParams = new TableRow.LayoutParams();
		// params.addRule(TableRow.LayoutParams.FILL_PARENT);
		rowParams.span = 2;

		row.addView(detailBtn, rowParams);
		row.addView(plotDataBtn, rowParams);

		// detailBtn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
		// LayoutParams.WRAP_CONTENT));
		// plotDataBtn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
		// LayoutParams.WRAP_CONTENT));

		row.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		table_view_mode.addView(row);
	}

	protected void loadData() {
		if (viewMode == 1)
			loadLatestData();
		else
			loadCustomizedRange();
	}

	private void loadLatestData() {
		if (vsName == null)
			return;
		controller.loadData(numLatest, vsName);
	}

	private void loadCustomizedRange() {
		Log.v(TAG, startTime.getTime() + " " + endTime.getTime());

		controller.loadRangeData(vsName, startTime.getTime(), endTime.getTime());
	}

	private void addTableViewModeCustomize() {
		table_view_mode = (TableLayout) findViewById(R.id.table_view_mode);
		table_view_mode.removeAllViews();

		// Row From
		TableRow row = new TableRow(this);
		TextView txt = new TextView(this);
		txt.setText("From: ");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		// Date time = new Date();
		startTime = new Date();
		startTime.setMinutes(startTime.getMinutes() - 1);
		endTime = new Date();

		// Start Time
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		txtStartTime = new TextView(this);
		txtStartTime.setText(formatter.format(startTime) + "");
		txtStartTime.setTextColor(Color.parseColor("#000000"));
		txtStartTime.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtStartTime);

		txtStartTime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new TimePickerDialog(context, startTimeSetListener, dateAndTime
						.get(Calendar.HOUR_OF_DAY) - 1, dateAndTime.get(Calendar.MINUTE),
						true).show();
			}
		});

		// Add space
		txt = new TextView(this);
		txt.setText("     ");
		row.addView(txt);

		// Start Date
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		// txtStartDate, txtStartTime
		txtStartDate = new TextView(this);
		txtStartDate.setText(formatter.format(startTime) + "");
		txtStartDate.setTextColor(Color.parseColor("#000000"));
		txtStartDate.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtStartDate);

		txtStartDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(context, startDateSetListener, dateAndTime
						.get(Calendar.YEAR), dateAndTime.get(Calendar.MONTH), dateAndTime
						.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		table_view_mode.addView(row);

		// Add a space row
		row = new TableRow(this);
		txt = new TextView(this);
		txt.setText("-");
		row.addView(txt);
		table_view_mode.addView(row);

		// Row To
		row = new TableRow(this);
		txt = new TextView(this);
		txt.setText("To");
		txt.setTextColor(Color.parseColor("#000000"));
		row.addView(txt);

		// End Time
		formatter = new SimpleDateFormat("HH:mm:ss");
		txtEndTime = new TextView(this);
		txtEndTime.setText(formatter.format(endTime) + "");
		txtEndTime.setTextColor(Color.parseColor("#000000"));
		txtEndTime.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtEndTime);

		txtEndTime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new TimePickerDialog(context, endTimeSetListener, dateAndTime
						.get(Calendar.HOUR_OF_DAY), dateAndTime.get(Calendar.MINUTE), true)
						.show();
			}
		});

		// Add space
		txt = new TextView(this);
		txt.setText("     ");
		row.addView(txt);

		// End Date
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		// txtStartDate, txtStartTime
		txtEndDate = new TextView(this);
		txtEndDate.setText(formatter.format(endTime) + "");
		txtEndDate.setTextColor(Color.parseColor("#000000"));
		txtEndDate.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtEndDate);

		txtEndDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(context, endDateSetListener, dateAndTime
						.get(Calendar.YEAR), dateAndTime.get(Calendar.MONTH), dateAndTime
						.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		table_view_mode.addView(row);

		// Row
		row = new TableRow(this);
		Button detailBtn = new Button(this);
		detailBtn.setTextSize(TEXT_SIZE + 2);
		detailBtn.setText("Detail");
		detailBtn.setTextColor(Color.parseColor("#000000"));
		detailBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		Button plotDataBtn = new Button(this);
		plotDataBtn.setTextSize(TEXT_SIZE + 2);
		plotDataBtn.setText("Plot data");
		plotDataBtn.setTextColor(Color.parseColor("#000000"));
		plotDataBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewChart();
			}
		});

		TableRow.LayoutParams params = new TableRow.LayoutParams();
		// params.addRule(TableRow.LayoutParams.FILL_PARENT);
		params.span = 2;

		row.addView(detailBtn, params);
		row.addView(plotDataBtn, params);
		row.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));
		table_view_mode.addView(row);

	}

	DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			startTime.setDate(dayOfMonth);
			startTime.setYear(year - 1900);
			startTime.setMonth(monthOfYear);
			updateStartLabel();
		}
	};

	TimePickerDialog.OnTimeSetListener startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			startTime.setHours(hourOfDay);
			startTime.setMinutes(minute);
			updateStartLabel();
		}
	};

	DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			endTime.setDate(dayOfMonth);
			endTime.setYear(year - 1900);
			endTime.setMonth(monthOfYear);
			updateEndLabel();
		}
	};

	TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			endTime.setHours(hourOfDay);
			endTime.setMinutes(minute);
			updateEndLabel();
		}
	};

	protected void updateEndLabel() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		txtEndTime.setText(formatter.format(endTime) + "");

		formatter = new SimpleDateFormat("dd/MM/yyyy");
		txtEndDate.setText(formatter.format(endTime) + "");
		// txtEndDate.setText(endTime.getTime() + "");

		loadCustomizedRange();
	}

	protected void updateStartLabel() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		txtStartTime.setText(formatter.format(startTime) + "");

		formatter = new SimpleDateFormat("dd/MM/yyyy");
		txtStartDate.setText(formatter.format(startTime) + "");
		// txtStartDate.setText(startTime.getTime() + "");

		loadCustomizedRange();
	}

	public void viewChart() {
		if (streamElements == null)
			return;
		if (streamElements.size() == 0)
			return;

		ArrayList<Double> data = new ArrayList<Double>();
		for (int i = 0; i < streamElements.size(); i++) {
			data.add((Double) streamElements.get(i).getData(fieldName));
			// Log.v(TAG, (Double) streamElements.get(i).getData(fieldName) + "");
		}

		// Log.v(TAG, data.size() + "");

		Intent intent = null;
		AbstractDemoChart chart = new SensorValuesChart(vsName, fieldName, data);
		intent = chart.execute(this);
		startActivity(intent);
	}

	// public void viewChart(View v) {
	// if (streamElements == null)
	// return;
	// if (streamElements.size() == 0)
	// return;
	//
	// Intent intent = null;
	// AbstractDemoChart chart = new SensorValuesChart(streamElements);
	// intent = chart.execute(this);
	// startActivity(intent);
	// }

	public void customizeStartEnd(View v) {
		Intent intent = new Intent(this, ActivityDateTimePicker.class);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem add = menu.add("Share");
		add.setIcon(R.drawable.ic_action_share).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(final MenuItem item) {
				shareOutputData();
				return false;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
//			Intent myIntent = new Intent(this, ActivityListVSNew.class);
//			this.startActivity(myIntent);
			finish();
			break;
		}
		return true;
	}

	protected void shareOutputData() {
		String text = getShareData();
		String subject = "Share " + spinnerVS.getSelectedItem().toString()
				+ " data";

		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, subject);
		i.putExtra(Intent.EXTRA_TEXT, text);

		copyTextToClipboard(text);
		Toast.makeText(context,
				"Customed message to post on Facebook is copied to clipboard!",
				Toast.LENGTH_SHORT).show();

		startActivity(Intent.createChooser(i, "Share TinyGSN data"));
	}

	@SuppressWarnings("deprecation")
	private void copyTextToClipboard(String text) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this
					.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(text);
		}
		else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) this
					.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = ClipData
					.newPlainText("simple text", text);
			clipboard.setPrimaryClip(clip);
		}
	}

	private void outputData(String out) {
		lblOutput.setText(out);
	}

	private String getShareData() {
		if (streamElements == null)
			return "";

		String out = "I'd like to share " + streamElements.size()
				+ " stream data of virtual senor '"
				+ spinnerVS.getSelectedItem().toString() + "'\n\n";
		for (int i = 0; i < streamElements.size(); i++) {
			out += streamElements.get(i).toString() + "\n";
		}
		return out;
	}

	private String getDataOutput() {
		if (streamElements == null)
			return "";

		String out = streamElements.size() + " stream data are loaded:\n";
		for (int i = 0; i < streamElements.size(); i++) {
			out += streamElements.get(i).toString() + "\n";
		}
		return out;
	}

	private void showDialogDetail() {
		String out = getDataOutput();

		DialogFragment newFragment = DetailedDataFragment.newInstance(out);
		newFragment.show(getSupportFragmentManager(), "dialog");
	}

	public static class DetailedDataFragment extends SherlockDialogFragment {
		String text;

		public static DetailedDataFragment newInstance(String text) {
			return new DetailedDataFragment(text);
		}

		public DetailedDataFragment(String text) {
			this.text = text;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.text, container, false);
			View tv = v.findViewById(R.id.text);
			((TextView) tv).setText(text);
			return v;
		}
	}
}
