package tinygsn.gui.android;

import java.util.ArrayList;
import tinygsn.gui.android.gcm.CommonUtilities;
import tinygsn.gui.android.gcm.ServerUtilities;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gcm.GCMRegistrar;

public class ActivitySubscribe extends SherlockFragmentActivity {
	public static String[] SERVER_LIST = { "http://10.0.2.2:22001",
			"http://data.permasense.ch", "http://montblanc.slf.ch:22001",
			"http://gsn.ijs.si" };

	static int TEXT_SIZE = 10;

	private EditText serverEditText = null;
	private EditText queryEditText = null;

	ArrayList<String> vsNameList = new ArrayList<String>();

	private AsyncTask<Void, Void, Void> mRegisterTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscribe);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(findViewById(R.id.select_server_btn));
		serverEditText = (EditText) findViewById(R.id.editText_server);
		queryEditText = (EditText) findViewById(R.id.editText_query);

	}

	private void registerOnServer(final String regId, final String serverURL) {
		final Context context = this;

		mRegisterTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				String query = queryEditText.getText().toString();
				query = query.replace(" ", "%20");

				ServerUtilities.registerWithQuery(context, serverURL, regId, query,
						"1.1111", "bcd");

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				mRegisterTask = null;
			}

		};
		mRegisterTask.execute(null, null, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Register").setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//		menu.add("UnRegister").setShowAsAction(
//				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		menu.add("Subscribe").setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		Log.e("ActivitySubscribe", "itemId=" + item.getTitle());

		int itemId = item.getItemId();
		if (item.getTitle().equals("UnRegister"))
			itemId = 1;
		else if (item.getTitle().equals("Register"))
			itemId = 2;

		switch (itemId) {
		case android.R.id.home:
			finish();
			break;

		case 0:
			// Subscribe

			final String regId = GCMRegistrar.getRegistrationId(this);

			if (regId.equals("")) {
				GCMRegistrar.register(this, CommonUtilities.SENDER_ID);

				Log.i("ActivitySubscribe",
						"GCMRegistrar.register(this, CommonUtilities.SENDER_ID);");
			}
			else
				registerOnServer(regId, serverEditText.getText().toString());

			break;

		case 1:
			GCMRegistrar.unregister(this);
			Log.e("ActivitySubscribe", "Unregister");
			Toast.makeText(this, "UnRegistered on GCM", Toast.LENGTH_SHORT).show();
			break;

		case 2:
			GCMRegistrar.register(this, CommonUtilities.SENDER_ID);
			Log.i("ActivitySubscribe",
					"GCMRegistrar.register(this, CommonUtilities.SENDER_ID);");
			Toast.makeText(this, "Registered on GCM", Toast.LENGTH_SHORT).show();

			break;
		}
		return true;
	}

	public void select_server(View view) {
		view.showContextMenu();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		for (String s : SERVER_LIST) {
			menu.add(s);
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		Toast.makeText(this, "Got click: " + item.toString(), Toast.LENGTH_SHORT)
				.show();
		serverEditText.setText(item.toString());
		return true;
	}

	@Override
	protected void onDestroy() {
		if (mRegisterTask != null) {
			mRegisterTask.cancel(true);
		}
		super.onDestroy();
	}
}
