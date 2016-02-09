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
 * File: gsn-tiny/src/tinygsn/gui/android/ActivityDateTimePicker.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

public class ActivityDateTimePicker extends AbstractActivity {
	TextView startLabel, endLabel;
	Date startTime, endTime;
	Calendar dateAndTime = Calendar.getInstance();

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.date_time_picker);

		startLabel = (TextView) findViewById(R.id.start_timeTxt);
		endLabel = (TextView) findViewById(R.id.end_timeTxt);

		startTime = new Date();
		endTime = new Date();

		updateStartLabel();
		updateEndLabel();
	}

	/********************************************************************************
	 * End
	 *
	 * @param v
	 */
	public void loadData(View v) {
		Log.v("", endTime.toString());
	}

	/********************************************************************************
	 * Start
	 *
	 * @param v
	 */
	public void chooseStartDate(View v) {
		new DatePickerDialog(ActivityDateTimePicker.this, startDateSetListener,
				                    dateAndTime.get(Calendar.YEAR), dateAndTime.get(Calendar.MONTH),
				                    dateAndTime.get(Calendar.DAY_OF_MONTH)).show();
	}

	public void chooseStartTime(View v) {
		new TimePickerDialog(ActivityDateTimePicker.this, startTimeSetListener,
				                    dateAndTime.get(Calendar.HOUR_OF_DAY),
				                    dateAndTime.get(Calendar.MINUTE), true).show();
	}

	private void updateStartLabel() {
		startLabel.setText(startTime.toString());
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

	/********************************************************************************
	 * End
	 *
	 * @param v
	 */
	public void chooseEndDate(View v) {
		new DatePickerDialog(ActivityDateTimePicker.this, endDateSetListener,
				                    dateAndTime.get(Calendar.YEAR), dateAndTime.get(Calendar.MONTH),
				                    dateAndTime.get(Calendar.DAY_OF_MONTH)).show();
	}

	public void chooseEndTime(View v) {
		new TimePickerDialog(ActivityDateTimePicker.this, endTimeSetListener,
				                    dateAndTime.get(Calendar.HOUR_OF_DAY),
				                    dateAndTime.get(Calendar.MINUTE), true).show();
	}

	private void updateEndLabel() {
		endLabel.setText(endTime.toString());
	}

	DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
		                      int dayOfMonth) {
			Log.v("time", year + "");
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
}