package tinygsn.gui.android;

import java.util.ArrayList;
import java.util.List;
import tinygsn.controller.AndroidControllerListSubscription;
import tinygsn.gui.android.gcm.CommonUtilities;
import tinygsn.gui.android.utils.SubscriptionListAdapter;
import tinygsn.gui.android.utils.SubscriptionRow;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.google.android.gcm.GCMRegistrar;

@SuppressLint("NewApi")
public class ActivityListSubscription extends SherlockActivity {

	private static final String TAG = "ActivityListSubscription";
	private ListView listViewSubscription;
	private Context context;
	Handler handlerData;
	AndroidControllerListSubscription controller;
	List<SubscriptionRow> subscriptionRowList;
	ArrayList<SubscriptionRow> dataList = new ArrayList<SubscriptionRow>();
	TextView numVS = null;

	private final Handler handler = new Handler();
	

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String data = intent.getExtras().getString(CommonUtilities.EXTRA_MESSAGE);
			String serverName = intent.getExtras().getString(
					CommonUtilities.EXTRA_SERVER_NAME);

			Log.i(TAG, "BroadcastReceiver onReceive: " + data);
			
			if (serverName == null)
				Toast.makeText(context, data, Toast.LENGTH_SHORT).show();
			else{
				controller.saveNewSubscriptionData(serverName, data);
				controller.loadListSubsData();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription_list);
		context = this;

		GCMRegistrar.checkDevice(this);
		GCMRegistrar.checkManifest(this);
		registerReceiver(mHandleMessageReceiver, new IntentFilter(
				CommonUtilities.DISPLAY_MESSAGE_ACTION));
		
//		final String regId = GCMRegistrar.getRegistrationId(this);
//		registerOnServer(regId);

		setUpController();
	}

	
	@SuppressWarnings("unchecked")
	public void setUpController() {

		handlerData = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				dataList = (ArrayList<SubscriptionRow>) msg.obj;
				TextView txt = (TextView) findViewById(R.id.txt);
				txt.setText(dataList.size() + " data are loaded!");
				renderLayout(dataList);
			};
		};

		controller = new AndroidControllerListSubscription(this);
		controller.setHandlerData(handlerData);
		controller.loadListSubsData();
	}

	private void renderLayout(ArrayList<SubscriptionRow> subscriptionRowList) {

		listViewSubscription = (ListView) findViewById(R.id.subscription_list);
		SubscriptionListAdapter dataListAdapter = new SubscriptionListAdapter(
				context, R.layout.subscription_row_item, subscriptionRowList,
				controller, this);
		listViewSubscription.setAdapter(dataListAdapter);
		dataListAdapter.notifyDataSetChanged();

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Add");
		add.setIcon(R.drawable.add).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

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

		final MenuItem deleteAll = menu.add("Delete all");
		deleteAll.setIcon(R.drawable.clear).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		deleteAll.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			public boolean onMenuItemClick(MenuItem item) {
				controller.deleteAll();
				controller.loadListSubsData();
				Toast.makeText(context, "Delete all subscribed data!",
						Toast.LENGTH_SHORT).show();

				return false;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			controller.markDataUnreadToRead();
			controller.closeDB();
			finish();
			break;
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mHandleMessageReceiver);
//		GCMRegistrar.onDestroy(this);
		super.onDestroy();
	}

	private void startVSActivity() {
		Intent myIntent = new Intent(this, ActivitySubscribe.class);
		this.startActivity(myIntent);
	}

	public void load_more(View view) {
		controller.loadMore();
	}
}