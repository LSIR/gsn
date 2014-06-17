package tinygsn.gui.android;

import java.util.ArrayList;
import tinygsn.controller.AndroidControllerListVS;
import tinygsn.model.wrappers.AbstractWrapper;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class ActivityListVS extends Activity{
	Context context = this;
	static int TEXT_SIZE = 10;

	TableLayout table = null;
	Handler handlerVS;
	AndroidControllerListVS controller;
	boolean firstRun = true;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Activate StrictMode
//		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//				.detectAll().penaltyLog().penaltyDeath().build());
//		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
//				.penaltyLog().penaltyDeath().build());

		AbstractWrapper.getWrapperList(this);
		
		renderLayout();
	}

	private void renderLayout() {
		table = new TableLayout(this);
		table.setStretchAllColumns(true);
		table.setShrinkAllColumns(true);

		TableRow rowTitle = new TableRow(this);
		TableRow rowNewVS = new TableRow(this);
		rowTitle.setGravity(Gravity.CENTER_HORIZONTAL);

		// title column/row
		TextView title = new TextView(this);
		title.setText("List of Virtual Sensor");
		title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.SERIF, Typeface.BOLD);

		TableRow.LayoutParams params = new TableRow.LayoutParams();
		params.span = 5;
		rowTitle.addView(title, params);

		Button addNewVS = new Button(this);
		addNewVS.setTextSize(TEXT_SIZE + 4);
		addNewVS.setText("Add New Vitual Sensor");
		addNewVS.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startVSActivity();
			}
		});

		rowNewVS.addView(addNewVS, params);

		table.addView(rowNewVS);
		table.addView(rowTitle);
		setContentView(table);

		// ~~~~~~~~~~~~~~~~Handle the result from Controller~~~~~~~~~~~~~~~~
		handlerVS = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				ArrayList<String> vsListName = (ArrayList<String>) msg.obj;
				for (String s : vsListName) {
					addRow(s, controller.getRunningState(s));
				}
			};
		};

		controller = new AndroidControllerListVS(this);
		controller.setHandlerVS(handlerVS);
		controller.loadListVSName();
//		controller.startActiveVS();
	}

	private void startVSActivity() {
		Intent myIntent = new Intent(this, ActivityVSConfig.class);
		this.startActivity(myIntent);
	}

	private void addRow(final String vsName, boolean running) {
		TableRow row = new TableRow(this);
		TextView vsNametxt = new TextView(this);
		vsNametxt.setText(vsName);

		final Switch runningSwitch = new Switch(this);
		runningSwitch.setTextOn("Running");
		runningSwitch.setTextOff("Disabled");
		runningSwitch.setChecked(running);
		runningSwitch.setTextSize(TEXT_SIZE);
		runningSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				controller.startStopVS(vsName, runningSwitch.isChecked());
				String state = "enabled";
				if (runningSwitch.isChecked() == false)
					state = "disabled";
				Toast.makeText(context, vsName + " is " + state + " successfully!",
						Toast.LENGTH_SHORT).show();
			}
		});

		// Button start = new Button(this);
		// start.setTextSize(TEXT_SIZE);
		// start.setText("Start");
		//
		// Button stop = new Button(this);
		// stop.setTextSize(TEXT_SIZE);
		// stop.setText("Stop");

		Button config = new Button(this);
		config.setTextSize(TEXT_SIZE);
		config.setText("Config");
		config.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Toast.makeText(context, "Config " + vsName,
				// Toast.LENGTH_SHORT).show();

			}
		});

		Button delete = new Button(this);
		delete.setTextSize(TEXT_SIZE);
		delete.setText("Delete");
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							controller.deleteVS(vsName);
							Toast.makeText(context, vsName + " is deleted!",
									Toast.LENGTH_SHORT).show();
							renderLayout();
							break;
						case DialogInterface.BUTTON_NEGATIVE:
							break;
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Are you sure to delete \'" + vsName + "\'?")
						.setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();
			}
		});

		row.addView(vsNametxt);
		row.addView(runningSwitch);
		row.addView(config);
		row.addView(delete);

		table.addView(row);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (firstRun == true) {
			firstRun = false;
		}
		else {
			renderLayout();
			// Toast.makeText(context, "Resumed", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 0, 0, "View Data");
		menu.add(Menu.NONE, 1, 1, "Publish Data");

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent viewDataIntent = new Intent(this, ActivityViewData.class);
			this.startActivity(viewDataIntent);
			return true;

		case 1:
			Intent publishDataIntent = new Intent(this, ActivityPublishData.class);
			this.startActivity(publishDataIntent);
			return true;
		}
		return true;
	}

}