package tinygsn.gui.android;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import tinygsn.beans.StreamElement;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.gui.android.utils.VSListAdapter;
import tinygsn.gui.android.utils.VSRow;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

@SuppressLint("NewApi")
public class ActivityListVSNew extends SherlockActivity {

	private static final String TAG = "ActivityListVSNew";
	private ListView listViewVS;
	private Context context;
	Handler handlerVS;
	AndroidControllerListVSNew controller;
	List<VSRow> vsRowList;
	ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();
	TextView numVS = null;

	private final Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vs_list);
//		setTheme(R.style.Sherlock___Theme_DarkActionBar);
		
		context = this;

		AbstractWrapper.getWrapperList(this);
		setUpController();
	}

	// ~~~~~~~~~~~~~~~~Handle the result from Controller~~~~~~~~~~~~~~~~
	public void setUpController() {
		handlerVS = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				vsList = (ArrayList<VirtualSensor>) msg.obj;
				renderLayout(vsList);
			};
		};

		controller = new AndroidControllerListVSNew(this);
		controller.setHandlerVS(handlerVS);
		controller.loadListVS();
		controller.startActiveVS();
//		Toast.makeText(context, "Started all active VS!", Toast.LENGTH_SHORT).show();
	}

	private void renderLayout(ArrayList<VirtualSensor> vsList) {
		vsRowList = new ArrayList<VSRow>();
		for (VirtualSensor vs : vsList) {
			DecimalFormat df = new DecimalFormat("#.##");

			String latest = "";
			StreamElement se = controller.loadLatestData(vs.getConfig().getName());
			if (se != null)
				for (String field : se.getFieldNames()) {
					latest += field + ": " + df.format(se.getData(field)) + "\n";
				}

			vsRowList.add(new VSRow(vs.getConfig().getName(), vs.getConfig()
					.getRunning(), latest));
		}

		// vsRowList.add(new VSRow("gps", false, "lon: 20.2 \nlat:30.1"));
		// vsRowList.add(new VSRow("temp", true, "temp: 25"));
		// vsRowList.add(new VSRow("gps", false, "lon: 20.2 \nlat:30.1"));
		// vsRowList.add(new VSRow("temp", true, "temp: 25"));
		// vsRowList.add(new VSRow("gps", false, "lon: 20.2 \nlat:30.1"));
		// vsRowList.add(new VSRow("temp", true, "temp: 25"));
		// vsRowList.add(new VSRow("gps", false, "lon: 20.2 \nlat:30.1"));
		// vsRowList.add(new VSRow("temp", true, "temp: 25"));

		listViewVS = (ListView) findViewById(R.id.vs_list);
		VSListAdapter vSListAdapter = new VSListAdapter(context,
				R.layout.vs_row_item, vsRowList, controller, this);
		listViewVS.setAdapter(vSListAdapter);
		vSListAdapter.notifyDataSetChanged();

		// TextView numVS = (TextView) findViewById(R.id.num_vs);
		// numVS.setText(vsRowList.size() + "");

		ActionBar actionBar = getSupportActionBar();
		actionBar.setCustomView(R.layout.actionbar_top); // load your layout
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_SHOW_CUSTOM); // show it

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		numVS = (TextView) actionBar.getCustomView().findViewById(R.id.num_vs);

		if (numVS == null) {
			Toast.makeText(context, "numVS is null", Toast.LENGTH_SHORT).show();
		}
		else {
			numVS.setText(vsRowList.size() + "");
			// BadgeView badge = new BadgeView(this, numVS);
			// badge.setText("2");
			// badge.show();
		}

		TextView lastUpdate = (TextView) actionBar.getCustomView().findViewById(
				R.id.lastUpdate);
		lastUpdate.setText("Last update:\n" + (new Date()).toString());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.add).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			// on selecting show add2 for 0.01s
			public boolean onMenuItemClick(final MenuItem item) {
				item.setIcon(R.drawable.add2);
				handler.postDelayed(new Runnable() {
					public void run() {
						item.setIcon(R.drawable.add);
					}
				}, 10);

				startVSActivity();

				return false;
			}
		});

		final MenuItem refresh = menu.add("Refresh");
		refresh.setIcon(R.drawable.ic_menu_refresh_holo_light).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		refresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			// on selecting show progress spinner for 1s
			public boolean onMenuItemClick(MenuItem item) {
				item.setActionView(R.layout.indeterminate_progress_action);
				handler.postDelayed(new Runnable() {
					public void run() {
						refresh.setActionView(null);
						controller.tinygsnStop();
						setUpController();
					}
				}, 50);
				return false;
			}
		});

		// menu.add("Search")
		// .setIcon(R.drawable.ic_search_inverse)
		// .setShowAsAction(
		// MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		//
		// menu.add("Search")
		// .setIcon(R.drawable.ic_search_inverse)
		// .setShowAsAction(
		// MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		//
		// menu.add("Search")
		// .setIcon(R.drawable.ic_search_inverse)
		// .setShowAsAction(
		// MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

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
		}
		return true;
	}

	@Override
	public void onResume() {
//		Toast.makeText(context, "Resuming", Toast.LENGTH_SHORT).show();
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause(); // Always call the superclass method first
//		Toast.makeText(context, "Stopping all active VS!", Toast.LENGTH_SHORT).show();
		controller.tinygsnStop();
	}

	private void startVSActivity() {
		Intent myIntent = new Intent(this, ActivityVSConfig.class);
		this.startActivity(myIntent);
	}
}