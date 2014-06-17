package tinygsn.gui.android;

import java.util.ArrayList;
import java.util.List;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.gui.android.utils.VSRow;
import tinygsn.model.vsensor.VirtualSensor;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.readystatesoftware.viewbadger.BadgeView;

public class ActivityHome extends SherlockActivity {

	Handler handlerVS;
	AndroidControllerListVSNew controller;
	List<VSRow> vsRowList;
	ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();
	TextView numVS = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		getSupportActionBar().hide();
		
//		ImageView subscribe = (ImageView) findViewById(R.id.imageViewSubscribe);
		TextView subscribe = (TextView) findViewById(R.id.tvSubscribe);
		BadgeView badge = new BadgeView(this, subscribe);
		badge.setBadgePosition(BadgeView.POSITION_BOTTOM_RIGHT); 
		badge.setText("2");
		badge.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		final MenuItem add = menu.add("Quit TinyGSN");

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(final MenuItem item) {
				finish();
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}
	
	public void open_listVSActivity(View view) {
		Intent myIntent = new Intent(this, ActivityListVSNew.class);
		this.startActivity(myIntent);
	}

	public void open_publishActivity(View view) {
		Intent myIntent = new Intent(this, ActivityPublishData.class);
		this.startActivity(myIntent);
	}

	public void open_PullActivity(View view) {
		Intent myIntent = new Intent(this, ActivityPullData.class);
		this.startActivity(myIntent);
	}

	public void open_SubscribeActivity(View view) {
		Intent myIntent = new Intent(this, ActivityListSubscription.class);
		this.startActivity(myIntent);
	}
	
	public void open_helpActivity(View view) {
		Intent myIntent = new Intent(this, ActivityHelp.class);
		this.startActivity(myIntent);
	}

	public void open_aboutActivity(View view) {
		Intent myIntent = new Intent(this, ActivityAboutUs.class);
		this.startActivity(myIntent);
	}
}