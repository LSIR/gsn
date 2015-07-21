package tinygsn.services;

import java.util.ArrayList;
import java.util.Map;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.AndroidAccelerometerWrapper;
import tinygsn.model.wrappers.AndroidActivityRecognitionWrapper;
import tinygsn.model.wrappers.AndroidGPSWrapper;
import tinygsn.model.wrappers.AndroidGyroscopeWrapper;
import tinygsn.model.wrappers.WifiWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class schedular extends IntentService {
	
	public static final int STATE_LOST = 0;
	public static final int STATE_GPS = 1;
	public static final int STATE_STATIONARY = 2;
	public static final int REASON_ACC = 0;
	public static final int REASON_WIFI = 1;
	public static final int REASON_GPS = 2;
	
	Intent intentGPS = null;
	Intent intentActivity = null;
	Intent intentWifi = null;
	Intent intentAcc = null;
	Intent intentGyro = null;
	
	WakeLock wakeLock = null;

	public schedular(String name) {
		super(name);
		setIntentRedelivery(true);
		try {
			accelometerWrapper = StaticData.getWrapperByName(accelometerType);
			gpsWrapper = StaticData.getWrapperByName(gpsType);
			gyroscopeWrapper = StaticData.getWrapperByName(gyroscopeType);
			wifiWrapper = StaticData.getWrapperByName(wifiType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
				
	}

	//constants:
	private int numLatest = 10;
	double accelometerThereshold = 1.3;
	int wifiCountThreshold = 15;
	int SchedulerSleepingTime = 1000*30;
	
	int SamplingRateAccelometerMoving = 3;
	int SamplingRateAccelometerStationary = 1;
    int SamplingRateAccelometerLost = 2;
    
	int SamplingRateGyroscopeMoving = 3;
	int SamplingRateGyroscopeStationary = 1;
    int SamplingRateGyroscopeLost = 2;

	int SamplingRateGPSMoving = 1;
	int SamplingRateGPSStationary = 0;
    int SamplingRateGPSLost = 2;

	int SamplingRateWifiMoving = 0;
	int SamplingRateWifiStationary = 2;
    int SamplingRateWifiLost = 1;

	int SamplingRateActivityMoving = 0;
	int SamplingRateActivityStationary = 0;
    int SamplingRateActivityLost = 0;
    //end of constants
    
	int machineState = STATE_LOST;
	int priMachineState = STATE_GPS;
	int reason = REASON_ACC;
	
	SqliteStorageManager storage = null;
	
	final String accelometerType = "tinygsn.model.wrappers.AndroidAccelerometerWrapper";
	AbstractWrapper accelometerWrapper;
	String accelometerVsName = null;

	final String gpsType = "tinygsn.model.wrappers.AndroidGPSWrapper";
	AbstractWrapper gpsWrapper;
	String gpsVsName = null;
	
	final String gyroscopeType = "tinygsn.model.wrappers.AndroidGyroscopeWrapper";
	AbstractWrapper gyroscopeWrapper;
	String gyroscopeVsName = null;
	
	AndroidActivityRecognitionWrapper activityWrapper = new AndroidActivityRecognitionWrapper();
	final String activityType = "tinygsn.model.wrappers.AndroidActivityRecognitionWrapper";
	String activityVsName = null;
	
	final String wifiType = "tinygsn.model.wrappers.WifiWrapper";
	AbstractWrapper wifiWrapper;
	String wifiVsName = null;

	
	public ArrayList<Integer> CalcRates()
	{

		int[] s = storage.getLatestState();
		machineState = s[0];
		reason = s[1];
		
		//calculation of parameters
		
		
		Log.d("tinygsn-scheduler", "State is: "+ machineState);
				
		long curTime = System.currentTimeMillis();
		double avgChangedAccelometer = 0;
		
		boolean gpsConstant = true;
		boolean isInKnownWifiAccess = true;
		
		ArrayList<StreamElement> accelometerResult = null;
		ArrayList<StreamElement> gpsResult = null;
		ArrayList<StreamElement> wifiResult = null;
		
		accelometerVsName = StaticData.findNameVs(accelometerType);
		gyroscopeVsName = StaticData.findNameVs(gyroscopeType);
		wifiVsName = StaticData.findNameVs(wifiType);
		gpsVsName = StaticData.findNameVs(gpsType);
		activityVsName = StaticData.findNameVs(activityType);
			
		if(wifiVsName != null)
		{
			wifiResult = storage.executeQueryGetLatestValues("vs_"+ wifiVsName, wifiWrapper.getFieldList(), wifiWrapper.getFieldType(), numLatest, curTime-120000);
			isInKnownWifiAccess = ContainsFamiliarWifis(wifiResult);
			Log.d("tinygsn-scheduler","is in knownwifi accesspoint: "+ isInKnownWifiAccess);
		}
		if(gpsVsName != null)
		{
			gpsResult = storage.executeQueryGetLatestValues("vs_"+gpsVsName, gpsWrapper.getFieldList(), gpsWrapper.getFieldType(), 180, curTime-180000);
			if(gpsResult != null && gpsResult.size() != 0)
			{
				if (gpsResult.get(gpsResult.size()-1).getTimeStamp() - gpsResult.get(0).getTimeStamp() > 120000){
					long longitude = Math.round(((Double)(gpsResult.get(0).getData("longitude")) * 1000));
					long latitude = Math.round(((Double)(gpsResult.get(0).getData("latitude")) * 1000));
					for(int i = 0; i < gpsResult.size(); i++)
					{
						if(Math.round(((Double)(gpsResult.get(i).getData("longitude")) * 1000)) != longitude || 
								Math.round(((Double)(gpsResult.get(i).getData("latitude")) * 1000)) != latitude)
							gpsConstant = false;
					}
				}else{
					gpsConstant = false;
				}
			}
			else
				gpsConstant = false;
			
			Log.d("tinygsn-scheduler","is GPS constant: "+ gpsConstant);
		}
		if(accelometerVsName != null)
		{
			accelometerResult =  storage.executeQueryGetLatestValues("vs_" + accelometerVsName, accelometerWrapper.getFieldList(), accelometerWrapper.getFieldType(), 32, curTime-30000);
			if(accelometerResult.size() > 1)
			{
				for(int i = 1; i < accelometerResult.size(); i++)
				{
					double changedAccelometer = Math.pow((Double)(accelometerResult.get(i).getData("x"))-(Double)(accelometerResult.get(i-1).getData("x")),2);
					changedAccelometer += Math.pow((Double)(accelometerResult.get(i).getData("y"))-(Double)(accelometerResult.get(i-1).getData("y")),2);
					changedAccelometer += Math.pow((Double)(accelometerResult.get(i).getData("z"))-(Double)(accelometerResult.get(i-1).getData("z")),2);
					avgChangedAccelometer += Math.sqrt(changedAccelometer);
				}
				avgChangedAccelometer = avgChangedAccelometer/accelometerResult.size();
			}
			Log.d("tinygsn-scheduler","average Acc change: "+ avgChangedAccelometer);
		}	
		
		//end of parameter calculation 
		//checking for next state
		
		if (gpsResult != null  && gpsResult.size() != 0){
			Log.d("tinygsn-scheduler", "Last GPS fix is : " + (curTime - gpsResult.get(0).getTimeStamp())/1000 + "s old.");
		}else{
			Log.d("tinygsn-scheduler", "no GPS results");
		}
		
		if (wifiResult != null && wifiResult.size() != 0){
			Log.d("tinygsn-scheduler", "Last wifi scan is : " + (curTime - wifiResult.get(0).getTimeStamp())/1000 + "s old.");
		}else{
			Log.d("tinygsn-scheduler", "no wifi results");
		}
		
		if (accelometerResult != null && accelometerResult.size() != 0){
			Log.d("tinygsn-scheduler", "Last acc scan is : " + (curTime - accelometerResult.get(0).getTimeStamp())/1000 + "s old.");
		}else{
			Log.d("tinygsn-scheduler", "no acc results");
		}
		
		
		switch(machineState)
		{
			
			case STATE_LOST:

				storage.setWrapperInfo(gyroscopeType, 15, SamplingRateGyroscopeLost);
				storage.setWrapperInfo(accelometerType, 15, SamplingRateAccelometerLost);
				storage.setWrapperInfo(gpsType, 15, SamplingRateGPSLost);
				storage.setWrapperInfo(wifiType, 60, SamplingRateWifiLost);
				storage.setWrapperInfo(activityType, 60, SamplingRateActivityLost);
				
				priMachineState = STATE_LOST;

				//changing stage
				if(gpsResult != null  && gpsResult.size() != 0) //gps fixed
				{
					machineState = STATE_GPS;
					Log.d("tinygsn-scheduler", "new state GPS");
				}
				else if(wifiResult != null && wifiResult.size() != 0 && isInKnownWifiAccess) 
				{
					machineState = STATE_STATIONARY;
					reason = REASON_WIFI;
					Log.d("tinygsn-scheduler", "new state STATIONARY (wifi)");
				}
				else if (accelometerResult != null && accelometerResult.size() != 0 && avgChangedAccelometer < accelometerThereshold)
				{
					machineState = STATE_STATIONARY;
					reason = REASON_ACC;
					Log.d("tinygsn-scheduler", "new state STATIONARY (acc)");
				}
				break;
			case STATE_GPS: 

				storage.setWrapperInfo(gyroscopeType, 15, SamplingRateGyroscopeMoving);
				storage.setWrapperInfo(accelometerType, 15, SamplingRateAccelometerMoving);
				storage.setWrapperInfo(gpsType, 15, SamplingRateGPSMoving);
				storage.setWrapperInfo(wifiType, 60, SamplingRateWifiMoving);
				storage.setWrapperInfo(activityType, 60, SamplingRateActivityMoving);		
				
				priMachineState = STATE_GPS;
				
				if(gpsResult == null || gpsResult.size() == 0) //gps lost
				{
					machineState = STATE_LOST;
					Log.d("tinygsn-scheduler", "new state LOST");
				}
				else if(gpsResult != null && gpsResult.size() != 0 && gpsConstant) 
				{
					machineState = STATE_STATIONARY;
					reason = REASON_GPS;
					Log.d("tinygsn-scheduler", "new state STATIONARY (gps)");
				}
				else if(accelometerResult != null && accelometerResult.size() != 0 && avgChangedAccelometer < accelometerThereshold) 
				{
					machineState = STATE_STATIONARY;
					reason = REASON_ACC;
					Log.d("tinygsn-scheduler", "new state STATIONARY (acc)");
				}
				break;
			case STATE_STATIONARY:

				storage.setWrapperInfo(gyroscopeType, 15, SamplingRateGyroscopeStationary);
				storage.setWrapperInfo(accelometerType, 15, SamplingRateAccelometerStationary);
				storage.setWrapperInfo(gpsType, 15, SamplingRateGPSStationary);
				storage.setWrapperInfo(wifiType, 60, SamplingRateWifiStationary);
				storage.setWrapperInfo(activityType, 60, SamplingRateActivityStationary);
	
				priMachineState = STATE_STATIONARY;

				if (reason == REASON_WIFI && (wifiResult == null || wifiResult.size() == 0 || !isInKnownWifiAccess))
				{
					machineState = STATE_LOST;
					Log.d("tinygsn-scheduler", "new state LOST");
				}
				else if (reason == REASON_ACC || reason == REASON_GPS)
				{
					if(wifiResult != null && wifiResult.size() != 0 && isInKnownWifiAccess) 
					{
						reason = REASON_WIFI;
						Log.d("tinygsn-scheduler", "new state STATIONARY (wifi)");
					}
					else if(accelometerResult != null && accelometerResult.size() != 0 && avgChangedAccelometer > accelometerThereshold)
					{
						machineState = STATE_LOST;
						Log.d("tinygsn-scheduler", "new state LOST");
					}
				}
		}
		storage.executeInsertSamples(machineState,reason);
		return null;
		
	}
	
	private boolean ContainsFamiliarWifis(ArrayList<StreamElement> wifiResult) {
		
		Map<Long, Integer> freqs = storage.getFrequencies();
		for(int i = 0; i < wifiResult.size(); i++)
		{
			Long k = ((Double)(wifiResult.get(i).getData("mac1"))).longValue() * 16777216 + ((Double)(wifiResult.get(i).getData("mac2"))).longValue();
			if( freqs.containsKey(k) && freqs.get(k) > wifiCountThreshold)
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (wakeLock != null){
			wakeLock.release();
		}
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		
		storage = new SqliteStorageManager(this);
		CalcRates();
 
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+SchedulerSleepingTime,PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
	}
	
	
}
