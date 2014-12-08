package tinygsn.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.gui.android.ActivityListVSNew;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AndroidAccelerometerWrapper;
import tinygsn.model.wrappers.AndroidActivityRecognitionWrapper;
import tinygsn.model.wrappers.AndroidGPSWrapper;
import tinygsn.model.wrappers.AndroidGyroscopeWrapper;
import tinygsn.model.wrappers.WifiWrapper;
import tinygsn.storage.db.SqliteStorageManager;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class schedular extends IntentService {
	
	Intent intentGPS = null;
	Intent intentActivity = null;
	Intent intentWifi = null;
	Intent intentAcc = null;
	Intent intentGyro = null;

	public schedular(String name) {
		super(name);
	}
	public schedular()
	{
		this("schedular");
		
	}

	//constants:
	final int timeThereshold = 1000*60*2;
	private int numLatest = 10;
	double accelometerThereshold = 10;
	int wifiCountThreshold = 15;
	
	int SchedulerSleepingTime = 1000*60;
	int SamplingRateAccelometerMoving = 1;
	int SamplingRateAccelometerStationary = 4;
    int SamplingRateAccelometerLost = 1;
    

	int SamplingRateGyroscopeMoving = 1;
	int SamplingRateGyroscopeStationary = 2;
    int SamplingRateGyroscopeLost = 1;

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
    
	private VSensorConfig config = null;
	int machineState = 0; //0 = lost, 1= moving 2= stationary
	int priMachineState = 1;
	SqliteStorageManager storage = null;
	
	
	AndroidAccelerometerWrapper accelometerWrapper = new AndroidAccelerometerWrapper();
	final String accelometerType = "tinygsn.model.wrappers.AndroidAccelerometerWrapper";
	String accelometerVsName = null;
	
	
	AndroidGPSWrapper gpsWrapper = new AndroidGPSWrapper();
	final String gpsType = "tinygsn.model.wrappers.AndroidGPSWrapper";
	String gpsVsName = null;
	
	
	AndroidGyroscopeWrapper gyroscopeWrapper = new AndroidGyroscopeWrapper();
	final String gyroscopeType = "tinygsn.model.wrappers.AndroidGyroscopeWrapper";
	String gyroscopeVsName = null;
	
	
	AndroidActivityRecognitionWrapper activityWrapper = new AndroidActivityRecognitionWrapper();
	final String activityType = "tinygsn.model.wrappers.AndroidActivityRecognitionWrapper";
	String activityVsName = null;
	
	
	WifiWrapper wifiWrapper = new WifiWrapper();
	final String wifiType = "tinygsn.model.wrappers.WifiWrapper";
	String wifiVsName = null;

	
	public void scheduleServices(ArrayList<Integer>rates)
	{
		
	}
	public ArrayList<Integer> CalcRates()
	{

		//calculation of parameters
		storage.executeInsertSamples(machineState);
		
		Log.d("tinygsn-scheduler", "State is: "+ machineState);
				
		long curTime = System.currentTimeMillis();
		double avgXChangedAccelometer = 0;
		double avgYChangedAccelometer = 0;
		double avgZChangedAccelometer = 0;
		
		double avgXChangedGyroscope = 0;
		double avgYChangedGyroscope = 0;
		double avgZChangedGyroscope = 0;
		
		boolean gpsConstant = true;
		boolean isInKnownWifiAccess = true;
		
		ArrayList<StreamElement> accelometerResult = null;
		ArrayList<StreamElement> gpsResult = null;
		ArrayList<StreamElement> wifiResult = null;
		ArrayList<StreamElement> gyroscopeResult = null;
		
		
		/*
		if(accelometerVsName == null && StaticData.findNameVs(accelometerType) != null)
			isAccelometerRecent = true;
		if(gyroscopeVsName == null && StaticData.findNameVs(gyroscopeType) != null)
			isGyroscopeRecent = true;
		if(gpsVsName == null && StaticData.findNameVs(gpsType) != null)
			isgpsRecent = true;
		if(wifiVsName == null && StaticData.findNameVs(wifiType) != null)
			iswifiRecent = true;
		if(activityVsName == null && StaticData.findNameVs(activityType) != null)
			isActivityRecent = true;
		*/
		
		accelometerVsName = StaticData.findNameVs(accelometerType);
		gyroscopeVsName = StaticData.findNameVs(gyroscopeType);
		wifiVsName = StaticData.findNameVs(wifiType);
		gpsVsName = StaticData.findNameVs(gpsType);
		activityVsName = StaticData.findNameVs(activityType);
			
		/*
		if (intentWifi == null){
		intentWifi = StaticData.getRunningIntentByName(wifiVsName);
		if(intentWifi != null)
		{
			config = StaticData.findConfig(StaticData.retrieveIDByName(wifiVsName));
			config.setRunning(true);
			intentWifi.putExtra("tinygsn.beans.config", config);
			startService(intentWifi);
			config = null;
		}
		}
		
		if (intentActivity == null){
		intentActivity = StaticData.getRunningIntentByName(activityVsName);
		if(intentActivity != null)
		{
			config = StaticData.findConfig(StaticData.retrieveIDByName(activityVsName));
			config.setRunning(true);
			intentActivity.putExtra("tinygsn.beans.config", config);
			startService(intentActivity);
			config = null;
		}
		}
		
		if (intentGPS == null){
		intentGPS = StaticData.getRunningIntentByName(gpsVsName);
		if(intentGPS != null)
		{
			config = StaticData.findConfig(StaticData.retrieveIDByName(gpsVsName));
			config.setRunning(true);
			intentGPS.putExtra("tinygsn.beans.config", config);
			startService(intentGPS);
		}
		}
		
		if (intentAcc == null){
		intentAcc = StaticData.getRunningIntentByName(accelometerVsName);
		if(intentAcc != null)
		{
			config = StaticData.findConfig(StaticData.retrieveIDByName(accelometerVsName));
			config.setRunning(true);
			intentAcc.putExtra("tinygsn.beans.config", config);
			startService(intentAcc);
			config = null;
		}
		}
		
		if (intentGyro == null){
		intentGyro = StaticData.getRunningIntentByName(gyroscopeVsName);
		if(intentGyro != null)
		{
			config = StaticData.findConfig(StaticData.retrieveIDByName(gyroscopeVsName));
			config.setRunning(true);
			intentGyro.putExtra("tinygsn.beans.config", config);
			startService(intentGyro);
			config = null;
		}
		}
		*/
		
		
		if(wifiVsName != null)
		{
			wifiResult = storage.executeQueryGetLatestValues("vs_"+ wifiVsName, wifiWrapper.getFieldList(), wifiWrapper.getFieldType(), numLatest);
			isInKnownWifiAccess = ContainsFamiliarWifis(wifiResult, curTime);
			Log.d("tinygsn-scheduler","is in knownwifi accesspoint: "+ isInKnownWifiAccess);
		}
		if(gpsVsName != null)
		{
			gpsResult = storage.executeQueryGetLatestValues("vs_"+gpsVsName, gpsWrapper.getFieldList(), gpsWrapper.getFieldType(), numLatest);
			if(gpsResult != null && gpsResult.size() != 0)
			{
				long longitude = Math.round(((Double)(gpsResult.get(0).getData("longitude")) * 1000));
				long latitude = Math.round(((Double)(gpsResult.get(0).getData("latitude")) * 1000));
				for(int i = 0; i < gpsResult.size(); i++)
				{
					if(Math.round(((Double)(gpsResult.get(i).getData("longitude")) * 1000)) != longitude || 
							Math.round(((Double)(gpsResult.get(i).getData("latitude")) * 1000)) != latitude)
						gpsConstant = false;
				}
			}
			else
				gpsConstant = false;
			
			Log.d("tinygsn-scheduler","is GPS constant: "+ gpsConstant);
		}
		if(gyroscopeVsName != null)
		{
			gyroscopeResult = storage.executeQueryGetLatestValues("vs_"+gyroscopeVsName, gyroscopeWrapper.getFieldList(), gyroscopeWrapper.getFieldType(), numLatest);
			if(gyroscopeResult.size() != 0)
			{
				for(int i = 0; i < gyroscopeResult.size(); i++)
				{
					avgXChangedGyroscope += Math.abs((Double)(gyroscopeResult.get(i).getData("x")));
					avgYChangedGyroscope += Math.abs((Double)(gyroscopeResult.get(i).getData("y")));
					avgZChangedGyroscope += Math.abs((Double)(gyroscopeResult.get(i).getData("z")));
				}
				avgXChangedGyroscope = avgXChangedGyroscope/gyroscopeResult.size();
				avgYChangedGyroscope = avgYChangedGyroscope/gyroscopeResult.size();
				avgZChangedGyroscope = avgZChangedGyroscope/gyroscopeResult.size();
			}
			
			Log.d("tinygsn-scheduler","average Gyro change: "+ avgXChangedGyroscope + "," + avgYChangedGyroscope + "," + avgZChangedGyroscope);
		}
		if(accelometerVsName != null)
		{
			accelometerResult =  storage.executeQueryGetLatestValues("vs_" + accelometerVsName, accelometerWrapper.getFieldList(), accelometerWrapper.getFieldType(), numLatest);
			if(accelometerResult.size() != 0)
			{
				for(int i = 0; i < accelometerResult.size(); i++)
				{
					avgXChangedAccelometer += Math.abs((Double)(accelometerResult.get(i).getData("x")));
					avgYChangedAccelometer += Math.abs((Double)(accelometerResult.get(i).getData("y")));
					avgZChangedAccelometer += Math.abs((Double)(accelometerResult.get(i).getData("z")));
				}
				avgXChangedAccelometer = avgXChangedAccelometer/accelometerResult.size();
				avgYChangedAccelometer = avgYChangedAccelometer/accelometerResult.size();
				avgZChangedAccelometer = avgZChangedAccelometer/accelometerResult.size();
			}
			Log.d("tinygsn-scheduler","average Acc change: "+ avgXChangedAccelometer + "," + avgYChangedAccelometer + "," + avgZChangedAccelometer);
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
			
			case 0://LOST

				storage.updateSamplingRate(gyroscopeType, SamplingRateGyroscopeLost);
				storage.updateSamplingRate(accelometerType,SamplingRateAccelometerLost);
				storage.updateSamplingRate(gpsType, SamplingRateGPSLost);
				storage.updateSamplingRate(wifiType, SamplingRateWifiLost);
				storage.updateSamplingRate(activityType, SamplingRateActivityLost);
				
				priMachineState = 0;

				//changing stage
				if((gpsResult != null  && gpsResult.size() != 0) && (curTime - gpsResult.get(0).getTimeStamp() < timeThereshold)) //gps fixed
				{
					machineState = 1;
					Log.d("tinygsn-scheduler", "new state GPS");
				}
				else if(((wifiResult != null && wifiResult.size() != 0) 
						&&(curTime - wifiResult.get(0).getTimeStamp() <timeThereshold ) && isInKnownWifiAccess) 
						|| ((accelometerResult != null && accelometerResult.size() != 0)
								&&((curTime - accelometerResult.get(0).getTimeStamp() < timeThereshold) &&
										((avgXChangedAccelometer < accelometerThereshold) && (avgYChangedAccelometer < accelometerThereshold) && (avgZChangedAccelometer < accelometerThereshold)))))
				{
					//accelometer low || known WIFI --> going to stationary state
					machineState = 2;
					Log.d("tinygsn-scheduler", "new state STATIONARY");
				}
				break;
			case 1: //GPS MOVING

				storage.updateSamplingRate(gyroscopeType, SamplingRateGyroscopeMoving);
				storage.updateSamplingRate(accelometerType, SamplingRateAccelometerMoving);
				storage.updateSamplingRate(gpsType, SamplingRateGPSMoving);
				storage.updateSamplingRate(wifiType, SamplingRateWifiMoving);
				storage.updateSamplingRate(activityType, SamplingRateActivityMoving);		
				
				priMachineState = 1;
				
				if((gpsResult != null && gpsResult.size() != 0) && (curTime - gpsResult.get(0).getTimeStamp() > timeThereshold)) //gps lost
				{
					machineState = 0;
					Log.d("tinygsn-scheduler", "new state LOST");
				}
				else if((gpsResult != null && gpsResult.size() != 0 && (curTime - gpsResult.get(0).getTimeStamp() < timeThereshold) && gpsConstant ) || (accelometerResult != null && accelometerResult.size() != 0 && (curTime - accelometerResult.get(0).getTimeStamp() < timeThereshold ) && ((avgXChangedAccelometer < accelometerThereshold) && (avgYChangedAccelometer < accelometerThereshold) && (avgZChangedAccelometer < accelometerThereshold)) ) )
				{
					//Accelometer low || gps constant --> stationary
					machineState = 2;
					Log.d("tinygsn-scheduler", "new state STATIONARY");
				}
				break;
			case 2:  //STATIONARY

				storage.updateSamplingRate(gyroscopeType, SamplingRateGyroscopeStationary);
				storage.updateSamplingRate(accelometerType,SamplingRateAccelometerStationary);
				storage.updateSamplingRate(gpsType, SamplingRateGPSStationary);
				storage.updateSamplingRate(wifiType, SamplingRateWifiStationary);
				storage.updateSamplingRate(activityType, SamplingRateActivityStationary);
	
				priMachineState = 2;

				if((wifiResult != null && wifiResult.size() != 0) 
						&& (((curTime - wifiResult.get(0).getTimeStamp() < timeThereshold)
								&& !isInKnownWifiAccess)
								//|| (curTime - wifiResult.get(0).getTimeStamp() > timeThereshold)
								)
								|| (accelometerResult != null && accelometerResult.size() != 0)
								&&(curTime - accelometerResult.get(0).getTimeStamp() < timeThereshold) &&
								((avgXChangedAccelometer > accelometerThereshold) || (avgYChangedAccelometer > accelometerThereshold) || (avgZChangedAccelometer > accelometerThereshold)))
				{
					machineState = 0;
					Log.d("tinygsn-scheduler", "new state LOST");
				}

		}
		return null;
		
	}
	
	private boolean ContainsFamiliarWifis(ArrayList<StreamElement> wifiResult,
			long curTime) {
		
		Map<Long, Integer> freqs = storage.getFrequencies();
		
		/*for(Long l : freqs.keySet()){
			Log.d("tinygsn-scheduler", "freq key:"+ l);
		}*/

		for(int i = 0; i < wifiResult.size(); i++)
		{
			
				Long k = ((Double)(wifiResult.get(i).getData("mac1"))).longValue() * 16777216 + ((Double)(wifiResult.get(i).getData("mac2"))).longValue();
				//Log.d("tinygsn-scheduler", "the key:"+ k);
				if ((curTime - wifiResult.get(i).getTimeStamp()) < timeThereshold){
				if( freqs.containsKey(k) && freqs.get(k) > wifiCountThreshold)
				{
					return true;
				}
			}
		}
		return false;
	}
	

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		
		while(true)
		{
			storage = new SqliteStorageManager(new ActivityListVSNew());
			CalcRates();
			try {
				Thread.sleep(SchedulerSleepingTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	 
		
	}
	
	
}
