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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityViewDataNew.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerVS;
import tinygsn.gui.android.chart.AbstractChart;
import tinygsn.gui.android.chart.SensorValuesChart;
import tinygsn.gui.android.utils.VSListAdapter;
import tinygsn.model.vsensor.AbstractVirtualSensor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import android.view.MenuItem;


public class ActivityViewData extends AbstractFragmentActivity {
	static int TEXT_SIZE = 10;


	private AndroidControllerVS controller = new AndroidControllerVS();

	private TextView lblOutput = null;
	private Spinner spinnerVS, spinnerField, spinnerViewMode;
	private TableLayout table_view_mode = null;

	private List<StreamElement> streamElements = null;


	private String vsName = null;
	private String vsNameFromExtra = null;
	protected String fieldName;

	private int numLatest = 10;
	protected int viewMode = 1;


	private TextView txtStartDate, txtStartTime, txtEndDate, txtEndTime;

	private Calendar startTime = Calendar.getInstance();
	private Calendar endTime = Calendar.getInstance();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_data);
		lblOutput = (TextView) findViewById(R.id.txbViewData);
		spinnerField = (Spinner) findViewById(R.id.spinner_field);
		spinnerVS = (Spinner) findViewById(R.id.spinner_vs);
		spinnerViewMode = (Spinner) findViewById(R.id.spinner_view_mode);
		lblOutput.setTextSize(TEXT_SIZE);

		startTime.add(Calendar.MINUTE, -1);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			vsNameFromExtra = extras.getString(VSListAdapter.EXTRA_VS_NAME);
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);

		loadVSList();
		loadViewMode();
		setListeners();
		addTableViewModeLatest();
	}

	public void loadVSList() {

		new AsyncTask<AndroidControllerVS, Void, List<String>>() {
			@Override
			protected List<String> doInBackground(AndroidControllerVS... params) {
				List<String> list = new ArrayList<String>();
				for (AbstractVirtualSensor vs : params[0].loadListVS()) {
					list.add(vs.getConfig().getName());
				}
				return list;
			}

			@Override
			protected void onPostExecute(List<String> result) {
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ActivityViewData.this, R.layout.spinner_item, result);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinnerVS.setAdapter(dataAdapter);

				int index = 0;
				if (vsNameFromExtra != null) {
					for (String s : result) {
						if (s.equals(vsNameFromExtra)) {
							spinnerVS.setSelection(index);
						}
						index++;
					}
				}
			}

		}.execute(controller);
	}

	private void loadViewMode() {
		List<String> list = new ArrayList<String>();
		list.add("Latest values");
		list.add("Customize time");

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerViewMode.setAdapter(dataAdapter);
	}

	public void setListeners() {

		spinnerVS.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				vsName = parent.getItemAtPosition(pos).toString();
				new AsyncTask<String, Void, List<String>>() {
					@Override
					protected List<String> doInBackground(String... params) {
						List<String> list = controller.loadListFields(params[0]);
						return list;
					}

					@Override
					protected void onPostExecute(List<String> result) {

						List<String> list = new ArrayList<String>();
						for (String s : result) {
							list.add(s);
						}
						ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ActivityViewData.this, R.layout.spinner_item, list);
						dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						spinnerField.setAdapter(dataAdapter);
					}

				}.execute(parent.getItemAtPosition(pos).toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		spinnerField.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if (vsName == null) return;
				fieldName = parent.getItemAtPosition(pos).toString();
				updateData();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		spinnerViewMode.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if (vsName == null) return;
				viewMode = pos + 1;
				if (viewMode == 1) {
					addTableViewModeLatest();
				} else {
					addTableViewModeCustomize();
				}
				updateData();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

	}

	private void updateData() {
		new AsyncTask<Void, Void, List<StreamElement>>() {
			@Override
			protected List<StreamElement> doInBackground(Void... params) {
				List<StreamElement> list = new ArrayList<StreamElement>();
				if (viewMode == 1) {
					list.addAll(controller.loadLatestData(numLatest, vsName));
				} else {
					list.addAll(controller.loadRangeData(vsName, startTime.getTimeInMillis(), endTime.getTimeInMillis()));
				}
				return list;
			}

			@Override
			protected void onPostExecute(List<StreamElement> result) {
				streamElements = result;
				StringBuilder sb = new StringBuilder();
				for (StreamElement se : result) {
					sb.append(se.toString());
				}
				lblOutput.setText(sb);
			}

		}.execute((Void) null);
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
		row.addView(txt);

		final EditText editText_numLatest = new EditText(this);
		editText_numLatest.setText("10");
		editText_numLatest.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText_numLatest.requestFocus();
		row.addView(editText_numLatest);

		editText_numLatest.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					numLatest = Integer.parseInt(editText_numLatest.getText().toString());
					updateData();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		});

		txt = new TextView(this);
		txt.setText(" latest values");
		row.addView(txt);

		txt = new TextView(this);
		txt.setText("            ");
		row.addView(txt);
		table_view_mode.addView(row);

		row = new TableRow(this);
		Button detailBtn = new Button(this);
		detailBtn.setText("Detail");
		detailBtn.setTextSize(TEXT_SIZE + 2);
		detailBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialogDetail();
			}
		});

		Button plotDataBtn = new Button(this);
		plotDataBtn.setText("Plot data");
		plotDataBtn.setTextSize(TEXT_SIZE + 2);

		plotDataBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewChart();
			}
		});

		TableRow.LayoutParams rowParams = new TableRow.LayoutParams();
		rowParams.span = 2;
		row.addView(detailBtn, rowParams);
		row.addView(plotDataBtn, rowParams);

		row.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				                                                LayoutParams.WRAP_CONTENT));
		table_view_mode.addView(row);
	}


	private void addTableViewModeCustomize() {
		table_view_mode = (TableLayout) findViewById(R.id.table_view_mode);
		table_view_mode.removeAllViews();

		// Row From
		TableRow row = new TableRow(this);
		TextView txt = new TextView(this);
		txt.setText("From: ");
		row.addView(txt);

		// Start Time
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		txtStartTime = new TextView(this);
		txtStartTime.setText(formatter.format(startTime.getTime()) + "");
		txtStartTime.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtStartTime);

		txtStartTime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new TimePickerDialog(ActivityViewData.this, startTimeSetListener, startTime.get(Calendar.HOUR_OF_DAY) - 1, startTime.get(Calendar.MINUTE), true).show();
			}
		});

		// Add space
		txt = new TextView(this);
		txt.setText("     ");
		row.addView(txt);

		// Start Date
		formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		// txtStartDate, txtStartTime
		txtStartDate = new TextView(this);
		txtStartDate.setText(formatter.format(startTime.getTime()) + "");
		txtStartDate.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtStartDate);

		txtStartDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(ActivityViewData.this, startDateSetListener, startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DAY_OF_MONTH)).show();
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
		row.addView(txt);

		// End Time
		formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		txtEndTime = new TextView(this);
		txtEndTime.setText(formatter.format(endTime.getTime()) + "");
		txtEndTime.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtEndTime);

		txtEndTime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new TimePickerDialog(ActivityViewData.this, endTimeSetListener, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true).show();
			}
		});

		// Add space
		txt = new TextView(this);
		txt.setText("     ");
		row.addView(txt);

		// End Date
		formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		// txtStartDate, txtStartTime
		txtEndDate = new TextView(this);
		txtEndDate.setText(formatter.format(endTime.getTime()) + "");
		txtEndDate.setBackgroundColor(Color.parseColor("#8dc63f"));
		row.addView(txtEndDate);

		txtEndDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(ActivityViewData.this, endDateSetListener, endTime.get(Calendar.YEAR), endTime.get(Calendar.MONTH), endTime.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		table_view_mode.addView(row);

		// Row
		row = new TableRow(this);
		Button detailBtn = new Button(this);
		detailBtn.setTextSize(TEXT_SIZE + 2);
		detailBtn.setText("Detail");
		detailBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialogDetail();
			}
		});

		Button plotDataBtn = new Button(this);
		plotDataBtn.setTextSize(TEXT_SIZE + 2);
		plotDataBtn.setText("Plot data");
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
		row.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		table_view_mode.addView(row);
	}


	DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			startTime.set(year, monthOfYear, dayOfMonth);
			updateStartLabel();
		}
	};

	TimePickerDialog.OnTimeSetListener startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			startTime.set(Calendar.MINUTE, minute);
			startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
			updateStartLabel();
		}
	};

	DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
		                      int dayOfMonth) {
			endTime.set(year, monthOfYear, dayOfMonth);
			updateEndLabel();
		}
	};

	TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
			endTime.set(Calendar.MINUTE, minute);
			updateEndLabel();
		}
	};

	protected void updateEndLabel() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		txtEndTime.setText(formatter.format(endTime.getTime()) + "");
		formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		txtEndDate.setText(formatter.format(endTime.getTime()) + "");
		updateData();
	}

	protected void updateStartLabel() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		txtStartTime.setText(formatter.format(startTime.getTime()) + "");
		formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		txtStartDate.setText(formatter.format(startTime.getTime()) + "");
		updateData();
	}

	public void viewChart() {
		if (streamElements == null)
			return;
		if (streamElements.size() == 0)
			return;

		Collections.sort(streamElements, new Comparator<StreamElement>() {
			@Override
			public int compare(StreamElement lhs, StreamElement rhs) {
				return (int) (lhs.getTimeStamp() - rhs.getTimeStamp());
			}
		});

		ArrayList<Long> x = new ArrayList<Long>();
		ArrayList<Double> y = new ArrayList<Double>();
		for (int i = 0; i < streamElements.size(); i++) {
			y.add((Double) streamElements.get(i).getData(fieldName));
			x.add(streamElements.get(i).getTimeStamp());
		}

		Intent intent = null;
		AbstractChart chart = new SensorValuesChart(vsName, fieldName, x, y);
		intent = chart.execute(this);
		startActivity(intent);
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
		newFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		newFragment.show(getSupportFragmentManager(), "dialog");
	}

	public static class DetailedDataFragment extends DialogFragment {
		String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public static DetailedDataFragment newInstance(String text) {
			DetailedDataFragment i = new DetailedDataFragment();
			i.setText(text);
			return i;
		}

		public DetailedDataFragment() {}

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
