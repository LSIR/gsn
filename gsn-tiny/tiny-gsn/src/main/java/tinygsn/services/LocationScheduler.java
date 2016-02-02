package tinygsn.services;

import android.content.Context;
import android.util.Log;

import android.view.View;
import android.widget.ListView;
import android.widget.Switch;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;

import tinygsn.gui.android.R;
import tinygsn.gui.android.TinyGSN;
import tinygsn.gui.android.utils.SensorRow;

import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.ToastUtils;

public class LocationScheduler extends AbstractScheduler {

	public static final int STATE_LOST = 0;
	public static final int STATE_GPS = 1;
	public static final int STATE_STATIONARY = 2;
	public static final int REASON_ACC = 0;
	public static final int REASON_WIFI = 1;
	public static final int REASON_GPS = 2;

	private static final String accelerometerType = "tinygsn.model.wrappers.AndroidAccelerometerWrapper";
	private static final String gpsType = "tinygsn.model.wrappers.AndroidGPSWrapper";
	private static final String gyroscopeType = "tinygsn.model.wrappers.AndroidGyroscopeWrapper";
	private static final String wifiType = "tinygsn.model.wrappers.WifiWrapper";

	private static final String[] wrappers = new String[]{accelerometerType, gpsType, gyroscopeType, wifiType};

	public final Class<? extends WrapperService> getSERVICE() {
		return LocationSchedulerService.class;
	}

	private static SqliteStorageManager storage = new SqliteStorageManager();

	public LocationScheduler(WrapperConfig wc) {
		super(wc);
	}

	public LocationScheduler() {
	}

	@Override
	public String[] getManagedSensors() {
		return wrappers;
	}


	public void runOnce() {
		int machineState = STATE_LOST;
		int reason = REASON_ACC;

		//constants:
		int numLatest = 10;
		double accelerometerThreshold = 1.3;

		int wifiCountThreshold = 15;

		int SamplingRateAccelerometerMoving = 3;
		int SamplingRateAccelerometerStationary = 1;
		int SamplingRateAccelerometerLost = 2;

		int SamplingRateGyroscopeMoving = 3;
		int SamplingRateGyroscopeStationary = 1;
		int SamplingRateGyroscopeLost = 2;

		int SamplingRateGPSMoving = 1;
		int SamplingRateGPSStationary = 0;
		int SamplingRateGPSLost = 2;

		int SamplingRateWifiMoving = 0;
		int SamplingRateWifiStationary = 2;
		int SamplingRateWifiLost = 1;
		//end of constants

		AbstractWrapper accelerometerWrapper;
		String accelerometerVsName = null;

		AbstractWrapper gpsWrapper;
		String gpsVsName = null;

		AbstractWrapper wifiWrapper;
		String wifiVsName = null;

		int[] s = storage.getLatestState();
		machineState = s[0];
		reason = s[1];

		try {
			//get the wrappers
			accelerometerWrapper = StaticData.getWrapperByName(accelerometerType);
			gpsWrapper = StaticData.getWrapperByName(gpsType);
			wifiWrapper = StaticData.getWrapperByName(wifiType);
			//get the first attached VS
			accelerometerVsName = storage.getVSfromSource(accelerometerType).get(0);
			wifiVsName = storage.getVSfromSource(wifiType).get(0);
			gpsVsName = storage.getVSfromSource(gpsType).get(0);
		} catch (Exception e) {
			// Get Switch to click on it to deactivate the LocationScheduler
			ListView listViewScheduler = (ListView) TinyGSN.getCurrentActivity().findViewById(R.id.scheduler_list);
			int count = listViewScheduler.getChildCount();
			for (int i = 0; i < count; i++) {
				View view = listViewScheduler.getChildAt(i);
				ArrayList<View> views = view.getTouchables();
				for (int j = 0; j < views.size(); j++) {
					SensorRow sensorRow = (SensorRow) listViewScheduler.getAdapter().getItem(j);
					if (sensorRow.getName().equals("tinygsn.services.LocationScheduler")) {
						final Switch sw = (Switch) views.get(j);
						sw.post(new Runnable() {
							@Override
							public void run() {
								sw.performClick();
							}
						});
						j = views.size();
						i = count;
					}
				}
			}

			// Warn the user with a Toast
			warnUserOfError();

			return;
		}

		//calculation of parameters

		Log.d("tinygsn-scheduler", "State is: " + machineState);

		long curTime = System.currentTimeMillis();
		double avgChangedAccelometer = 0;
		boolean gpsConstant = true;
		boolean isInKnownWifiAccess = true;

		ArrayList<StreamElement> accelerometerResult = null;
		ArrayList<StreamElement> gpsResult = null;
		ArrayList<StreamElement> wifiResult = null;

		if (wifiVsName != null) {
			wifiResult = storage.executeQueryGetLatestValues("vs_" + wifiVsName, wifiWrapper.getFieldList(), wifiWrapper.getFieldType(), numLatest, curTime - 120000);
			isInKnownWifiAccess = ContainsFamiliarWifis(wifiResult, wifiCountThreshold);
			Log.d("tinygsn-scheduler", "is in known wifi access point: " + isInKnownWifiAccess);
		}
		if (gpsVsName != null) {
			gpsResult = storage.executeQueryGetLatestValues("vs_" + gpsVsName, gpsWrapper.getFieldList(), gpsWrapper.getFieldType(), 180, curTime - 180000);
			if (gpsResult != null && gpsResult.size() != 0) {
				if (gpsResult.get(gpsResult.size() - 1).getTimeStamp() - gpsResult.get(0).getTimeStamp() > 120000) {
					long longitude = Math.round(((Double) (gpsResult.get(0).getData("longitude")) * 1000));
					long latitude = Math.round(((Double) (gpsResult.get(0).getData("latitude")) * 1000));
					for (int i = 0; i < gpsResult.size(); i++) {
						if (Math.round(((Double) (gpsResult.get(i).getData("longitude")) * 1000)) != longitude ||
								    Math.round(((Double) (gpsResult.get(i).getData("latitude")) * 1000)) != latitude)
							gpsConstant = false;
					}
				} else {
					gpsConstant = false;
				}
			} else
				gpsConstant = false;

			Log.d("tinygsn-scheduler", "is GPS constant: " + gpsConstant);
		}
		if (accelerometerVsName != null) {
			accelerometerResult = storage.executeQueryGetLatestValues("vs_" + accelerometerVsName, accelerometerWrapper.getFieldList(), accelerometerWrapper.getFieldType(), 32, curTime - 30000);
			if (accelerometerResult.size() > 1) {
				for (int i = 1; i < accelerometerResult.size(); i++) {
					double changedAccelerometer = Math.pow((Double) (accelerometerResult.get(i).getData("x")) - (Double) (accelerometerResult.get(i - 1).getData("x")), 2);
					changedAccelerometer += Math.pow((Double) (accelerometerResult.get(i).getData("y")) - (Double) (accelerometerResult.get(i - 1).getData("y")), 2);
					changedAccelerometer += Math.pow((Double) (accelerometerResult.get(i).getData("z")) - (Double) (accelerometerResult.get(i - 1).getData("z")), 2);
					avgChangedAccelometer += Math.sqrt(changedAccelerometer);
				}
				avgChangedAccelometer = avgChangedAccelometer / accelerometerResult.size();
			}
			Log.d("tinygsn-scheduler", "average Acc change: " + avgChangedAccelometer);
		}

		//end of parameter calculation
		//checking for next state

		if (gpsResult != null && gpsResult.size() != 0) {
			Log.d("tinygsn-scheduler", "Last GPS fix is : " + (curTime - gpsResult.get(0).getTimeStamp()) / 1000 + "s old.");
		} else {
			Log.d("tinygsn-scheduler", "no GPS results");
		}

		if (wifiResult != null && wifiResult.size() != 0) {
			Log.d("tinygsn-scheduler", "Last wifi scan is : " + (curTime - wifiResult.get(0).getTimeStamp()) / 1000 + "s old.");
		} else {
			Log.d("tinygsn-scheduler", "no wifi results");
		}

		if (accelerometerResult != null && accelerometerResult.size() != 0) {
			Log.d("tinygsn-scheduler", "Last acc scan is : " + (curTime - accelerometerResult.get(0).getTimeStamp()) / 1000 + "s old.");
		} else {
			Log.d("tinygsn-scheduler", "no acc results");
		}


		switch (machineState) {

			case STATE_LOST:

				storage.setWrapperInfo(gyroscopeType, 15, SamplingRateGyroscopeLost);
				storage.setWrapperInfo(accelerometerType, 15, SamplingRateAccelerometerLost);
				storage.setWrapperInfo(gpsType, 15, SamplingRateGPSLost);
				storage.setWrapperInfo(wifiType, 60, SamplingRateWifiLost);


				//changing stage
				if (gpsResult != null && gpsResult.size() != 0) //gps fixed
				{
					machineState = STATE_GPS;
					Log.d("tinygsn-scheduler", "new state GPS");
				} else if (wifiResult != null && wifiResult.size() != 0 && isInKnownWifiAccess) {
					machineState = STATE_STATIONARY;
					reason = REASON_WIFI;
					Log.d("tinygsn-scheduler", "new state STATIONARY (wifi)");
				} else if (accelerometerResult != null && accelerometerResult.size() != 0 && avgChangedAccelometer < accelerometerThreshold) {
					machineState = STATE_STATIONARY;
					reason = REASON_ACC;
					Log.d("tinygsn-scheduler", "new state STATIONARY (acc)");
				}
				break;
			case STATE_GPS:

				storage.setWrapperInfo(gyroscopeType, 15, SamplingRateGyroscopeMoving);
				storage.setWrapperInfo(accelerometerType, 15, SamplingRateAccelerometerMoving);
				storage.setWrapperInfo(gpsType, 15, SamplingRateGPSMoving);
				storage.setWrapperInfo(wifiType, 60, SamplingRateWifiMoving);


				if (gpsResult == null || gpsResult.size() == 0) //gps lost
				{
					machineState = STATE_LOST;
					Log.d("tinygsn-scheduler", "new state LOST");
				} else if (gpsResult != null && gpsResult.size() != 0 && gpsConstant) {
					machineState = STATE_STATIONARY;
					reason = REASON_GPS;
					Log.d("tinygsn-scheduler", "new state STATIONARY (gps)");
				} else if (accelerometerResult != null && accelerometerResult.size() != 0 && avgChangedAccelometer < accelerometerThreshold) {
					machineState = STATE_STATIONARY;
					reason = REASON_ACC;
					Log.d("tinygsn-scheduler", "new state STATIONARY (acc)");
				}
				break;
			case STATE_STATIONARY:

				storage.setWrapperInfo(gyroscopeType, 15, SamplingRateGyroscopeStationary);
				storage.setWrapperInfo(accelerometerType, 15, SamplingRateAccelerometerStationary);
				storage.setWrapperInfo(gpsType, 15, SamplingRateGPSStationary);
				storage.setWrapperInfo(wifiType, 60, SamplingRateWifiStationary);


				if (reason == REASON_WIFI && (wifiResult == null || wifiResult.size() == 0 || !isInKnownWifiAccess)) {
					machineState = STATE_LOST;
					Log.d("tinygsn-scheduler", "new state LOST");
				} else if (reason == REASON_ACC || reason == REASON_GPS) {
					if (wifiResult != null && wifiResult.size() != 0 && isInKnownWifiAccess) {
						reason = REASON_WIFI;
						Log.d("tinygsn-scheduler", "new state STATIONARY (wifi)");
					} else if (accelerometerResult != null && accelerometerResult.size() != 0 && avgChangedAccelometer > accelerometerThreshold) {
						machineState = STATE_LOST;
						Log.d("tinygsn-scheduler", "new state LOST");
					}
				}
		}
		storage.executeInsertSamples(machineState, reason);
		return;

	}

	private boolean ContainsFamiliarWifis(ArrayList<StreamElement> wifiResult, int wifiCountThreshold) {

		Map<Long, Integer> freqs = storage.getFrequencies();
		for (int i = 0; i < wifiResult.size(); i++) {
			Long k = ((Double) (wifiResult.get(i).getData("mac1"))).longValue() * 16777216 + ((Double) (wifiResult.get(i).getData("mac2"))).longValue();
			if (freqs.containsKey(k) && freqs.get(k) > wifiCountThreshold) {
				return true;
			}
		}
		return false;
	}

	private void warnUserOfError() {
		Context context = StaticData.globalContext.getApplicationContext();
		CharSequence text = "The Location scheduler needs existing virtual sensors for GPS, Wifi and Accelerometer.";
		int duration = Toast.LENGTH_LONG;

		ToastUtils.showToastInUiThread(context, text, duration);
	}

	public static class LocationSchedulerService extends WrapperService {
		public LocationSchedulerService() {
			super("LocationScheduler");
			setIntentRedelivery(true);
		}
	}


}
