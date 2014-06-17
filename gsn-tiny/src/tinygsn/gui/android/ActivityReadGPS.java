package tinygsn.gui.android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Reading GPS data on Android
 * 
 * @author Do Ngoc Hoan
 */
public class ActivityReadGPS extends Activity {

	private static final String TAG = "ReadGPS Activity";
	
	private LocationManager myLocationManager;
	private LocationListener myLocationListener;

	EditText txbTimer = null;
	Button btnSet = null;
	Button btnLoad = null;
	TextView lblOutput = null;
	TextView lblLastUpdate = null;
	Date date;

	// Set timer 5s by default
	final Handler handler = new Handler();
	private Runnable runnable;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_gps);

		lblOutput = (TextView) findViewById(R.id.txbOutput);
		txbTimer = (EditText) findViewById(R.id.txbTimer);
		lblLastUpdate = (TextView) findViewById(R.id.ViewLastUpdate);

		Log.v(TAG, "Logger started successfully");
		
		// Button to set timer
		btnSet = (Button) findViewById(R.id.btnSet);
		btnSet.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				scheduleUpdating1(1000 * Integer.valueOf(txbTimer.getText().toString()));
			}
		});

		// Button to load GPS data manually
		btnLoad = (Button) findViewById(R.id.btnLoad);
		btnLoad.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getLastKnownLocation();
			}
		});

		// Get Location service
		myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Register a LocationListener
		myLocationListener = new MyLocationListener();
		// Update GPS data every 0 ms and 0 meter
		myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, myLocationListener);

		// Load GPS data at start-up
		getLastKnownLocation();
		// Auto get GPS data after 5s
		scheduleUpdating1(5000);
	}

	/*
	 * Timer in Android using Handler
	 * http://saeedsiam.blogspot.com/2009/03/timer-in-android-better-way.html
	 */
	private void scheduleUpdating1(final int interval) {
		try {
			handler.removeCallbacks(runnable);
		}
		catch (Exception e) {
			Log.e("tag", e.getMessage());
		}

		runnable = new Runnable() {
			public void run() {
				getLastKnownLocation();
				handler.postDelayed(this, interval);
			}
		};
		runnable.run();
	}

	protected void getLastKnownLocation() {
		date = new Date();

		try {
			// Get the current location
			Location location = myLocationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (location == null) {
				lblOutput.setText("There is no signal!");
			}
			else {
				String longitude = String.valueOf(location.getLongitude());
				String latitude = String.valueOf(location.getLatitude());

				lblOutput.setText("Longitude: " + longitude);
				lblOutput.append("\nLatitude: " + latitude);

				// Append gps data to file
				SaveDataToFile(getTime(date) + "," + longitude + "," + latitude + "");
			}
		}
		catch (Exception e) {
			lblOutput.setText("Error! There is no signal.");
			// lblOutput.append(e.getMessage().toString());
		}

		lblLastUpdate.setText("Last updated: \n" + date.toString());
	}

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			lblOutput.setText("Longitude: ");
			lblOutput.append(String.valueOf(location.getLongitude()));

			lblOutput.append("\nLatitude: ");
			lblOutput.append(String.valueOf(location.getLatitude()));

			lblOutput.append("\nUpdate through LocationListener.");

			date = new Date();
			lblLastUpdate.setText("Last updated: \n" + date.toString());
		}

		public void onProviderDisabled(String provider) {
			lblOutput.append("\nProviderDisabled.");
		}

		public void onProviderEnabled(String provider) {
			lblOutput.append("\nProviderEnabled.");
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			lblOutput.append("\nStatusChanged.");
		}
	};

	private void SaveDataToFile(String data) {
		try {
			File root = Environment.getExternalStorageDirectory();
			String dir = root + "/epfl/gps/";

			if (root.canWrite()) {

				// Write 1 file per day
				Date date = new Date();

				// Create directory
				File gpsfile = new File(dir);
				if (!gpsfile.exists()) {
					gpsfile.mkdir();
				}

				// Create file
				gpsfile = new File(dir + getDate(date) + "-gps.txt");
				if (!gpsfile.exists())
					gpsfile.createNewFile();

				// Append file
				FileWriter txtWriter = new FileWriter(gpsfile, true);
				BufferedWriter out = new BufferedWriter(txtWriter);
				out.write(data + "\n");
				out.close();
			}
			else {
				lblOutput.append("\nCan not write to:" + dir);
			}
			lblOutput.append("\nSave file successful.");
		}
		catch (IOException e) {
			Log.e("tag", "Could not write file " + e.getMessage());
			lblOutput.append("\nSave file fail: " + e.getMessage());
		}
	}

	private String getDate(Date date) {
		String d = String.valueOf(date.getYear() + 1900) + "-"
				+ String.valueOf(date.getMonth() + 1) + "-"
				+ String.valueOf(date.getDate());
		return d;
	}

	private String getTime(Date date) {
		String time = String.valueOf(date.getHours()) + ":"
				+ String.valueOf(date.getMinutes()) + ":"
				+ String.valueOf(date.getSeconds());
		return time;
	}
}