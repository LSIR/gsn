package tinygsn.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.gui.android.ActivityListVSNew;
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

public class schedular extends IntentService {

	public schedular(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	public schedular()
	{
		super("schedulat");
	}

	//constants:
	final int timeThereshold = 1000*60*2;
	private int numLatest = 10;
	double accelometerThereshold = 10;
	int SchedulerSleepingTime = 1000*60;
	int SamplingRateAccelometerMoving = 1;
	int SamplingRateAccelometerStationary = 2;
    int SamplingRateAccelometerLost = 0;
    

	int SamplingRateGyroscopeMoving = 1;
	int SamplingRateGyroscopeStationary = 2;
    int SamplingRateGyroscopeLost = 0;

	int SamplingRateGPSMoving = 1;
	int SamplingRateGPSStationary = 2;
    int SamplingRateGPSLost = 0;

	int SamplingRateWifiMoving = 1;
	int SamplingRateWifiStationary = 2;
    int SamplingRateWifiLost = 0;

	int SamplingRateActivityMoving = 1;
	int SamplingRateActivityStationary = 2;
    int SamplingRateActivityLost = 0;
	
    
    //end of constants
    
	private VSensorConfig config = null;
	Context context = new ActivityListVSNew(); //TODO set this parameter
	int machineState = 1; //0 = lost, 1= moving 2= stationary
	int priMachineState = -1;
	SqliteStorageManager storage = new SqliteStorageManager(this.context);
	
	
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
		boolean iswifiRecent = false;
		boolean isActivityRecent = false;
		boolean isGyroscopeRecent = false;
		boolean isgpsRecent = false;
		boolean isAccelometerRecent = false;
		
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
		
		
		Intent intentGPS = null;
		Intent intentActivity = null;
		Intent intentWifi = null;
		
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
		
		accelometerVsName = StaticData.findNameVs(accelometerType);
		gyroscopeVsName = StaticData.findNameVs(gyroscopeType);
		wifiVsName = StaticData.findNameVs(wifiType);
		gpsVsName = StaticData.findNameVs(gpsType);
		activityVsName = StaticData.findNameVs(activityType);
		
		
		if(wifiVsName != null)
		{
			wifiResult = storage.executeQueryGetLatestValues("vs_"+ wifiVsName, wifiWrapper.getFieldList(), wifiWrapper.getFieldType(), numLatest);
			isInKnownWifiAccess = ContainsFamiliarWifis(wifiResult, curTime);
		}
		if(StaticData.findNameVs(gpsType) != null)
		{
			gpsResult = storage.executeQueryGetLatestValues("vs_"+StaticData.findNameVs(gpsType), gpsWrapper.getFieldList(), gpsWrapper.getFieldType(), numLatest);
			if(gpsResult != null && gpsResult.size() != 0)
			{
				double longitude = Double.parseDouble(gpsResult.get(0).getData("longitude").toString());
				double latitude = Double.parseDouble(gpsResult.get(0).getData("latitude").toString());
				for(int i = 0; i < gpsResult.size(); i++)
				{
					if(Double.parseDouble(gpsResult.get(i).getData("longitude").toString()) != longitude || Double.parseDouble(gpsResult.get(0).getData("latitude").toString()) != latitude)
						gpsConstant = false;
				}
			}
			else
				gpsConstant = false;
		}
		if(StaticData.findNameVs(gyroscopeType) != null)
		{
			gyroscopeResult = storage.executeQueryGetLatestValues("vs_"+StaticData.findNameVs(gyroscopeType), gyroscopeWrapper.getFieldList(), gyroscopeWrapper.getFieldType(), numLatest);
			if(gyroscopeResult.size() != 0)
			{
				for(int i = 0; i < gyroscopeResult.size(); i++)
				{
					avgXChangedGyroscope += Math.abs(Double.parseDouble(gyroscopeResult.get(i).getData("x").toString()));
					avgYChangedGyroscope += Math.abs(Double.parseDouble(gyroscopeResult.get(i).getData("y").toString()));
					avgZChangedGyroscope += Math.abs(Double.parseDouble(gyroscopeResult.get(i).getData("z").toString()));
				}
				avgXChangedGyroscope = avgXChangedGyroscope/gyroscopeResult.size();
				avgYChangedGyroscope = avgYChangedGyroscope/gyroscopeResult.size();
				avgZChangedGyroscope = avgZChangedGyroscope/gyroscopeResult.size();
			}
		}
		if(accelometerVsName != null)
		{
			accelometerResult =  storage.executeQueryGetLatestValues("vs_" + accelometerVsName, accelometerWrapper.getFieldList(), accelometerWrapper.getFieldType(), numLatest);
			if(accelometerResult.size() != 0)
			{
				for(int i = 0; i < accelometerResult.size(); i++)
				{
					avgXChangedAccelometer += Math.abs(Double.parseDouble(accelometerResult.get(i).getData("x").toString()));
					avgYChangedAccelometer += Math.abs(Double.parseDouble(accelometerResult.get(i).getData("y").toString()));
					avgZChangedAccelometer += Math.abs(Double.parseDouble(accelometerResult.get(i).getData("z").toString()));
				}
				avgXChangedAccelometer = avgXChangedAccelometer/accelometerResult.size();
				avgYChangedAccelometer = avgYChangedAccelometer/accelometerResult.size();
				avgZChangedAccelometer = avgZChangedAccelometer/accelometerResult.size();
			}
		}	
		
		//end of parameter calculation 
		//checking for next state
		switch(machineState)
		{
			
			case 0:
				if(priMachineState != 0 || isAccelometerRecent || isActivityRecent || isGyroscopeRecent || isgpsRecent || iswifiRecent)
				{
					
					if(gyroscopeVsName != null || isGyroscopeRecent)
						storage.updateSamplingRate(gyroscopeType, SamplingRateGyroscopeLost);
					if(accelometerVsName != null || isAccelometerRecent)
						storage.updateSamplingRate(accelometerType,SamplingRateAccelometerLost);
					if(gpsVsName != null || isgpsRecent)
						storage.updateSamplingRate(gpsType, SamplingRateGPSLost);
					if(wifiVsName != null || iswifiRecent)
						storage.updateSamplingRate(wifiType, SamplingRateWifiLost);
					if(activityVsName != null || isAccelometerRecent)
						storage.updateSamplingRate(activityType, SamplingRateActivityLost);
					
					if(priMachineState == 1) // in this case we have to turn on the wifi
					{
						String wifiVSName = StaticData.findNameVs(wifiType);
						intentWifi = StaticData.getRunningIntentByName(wifiVSName);
						if(intentWifi != null)
						{
							config = StaticData.findConfig(StaticData.retrieveIDByName(wifiVSName));
							intentWifi.putExtra("tinygsn.beans.config", config);
							startService(intentWifi);
							config = null;
						}
						
					}
					else if(priMachineState == 2) // in this case we have to turn off the activity
					{
						String activityVSName = StaticData.findNameVs(activityType);
						intentActivity = StaticData.getRunningIntentByName(activityVSName);
						if(intentActivity != null)
						{
							config = StaticData.findConfig(StaticData.retrieveIDByName(activityVSName));
							intentActivity.putExtra("tinygsn.beans.config", config);
							startService(intentActivity);
							config = null;
						}
					}
		
					String gpsVSName = StaticData.findNameVs(gpsType);
					intentGPS = StaticData.getRunningIntentByName(gpsVSName);
					if(intentGPS != null)
					{
						config = StaticData.findConfig(StaticData.retrieveIDByName(gpsVSName));
						intentGPS.putExtra("tinygsn.beans.config", config);
						startService(intentGPS);
					}
					priMachineState = 0;
					
				}
				//changing stage
				if((gpsResult != null  && gpsResult.size() != 0) && (curTime - gpsResult.get(0).getTimeStamp() < timeThereshold)) //gps fixed
				{
					priMachineState = 0;
					machineState = 1;
				}
				else if(((wifiResult != null && wifiResult.size() != 0) 
						&&(curTime - wifiResult.get(0).getTimeStamp() <timeThereshold ) && isInKnownWifiAccess) 
						|| ((accelometerResult != null && accelometerResult.size() != 0)
								&&((curTime - accelometerResult.get(0).getTimeStamp() < timeThereshold) &&
										((avgXChangedAccelometer < accelometerThereshold) && (avgYChangedAccelometer < accelometerThereshold) && (avgZChangedAccelometer < accelometerThereshold)))))
				{
					//accelometer low || known WIFI --> going to stationary state
					priMachineState = 0;
					machineState = 2;
				}
				break;
			case 1:
				if(priMachineState != 1 || isAccelometerRecent || isActivityRecent || isGyroscopeRecent || isgpsRecent || iswifiRecent)
				{
					if(gyroscopeVsName != null || isGyroscopeRecent)
						storage.updateSamplingRate(gyroscopeType, SamplingRateGyroscopeMoving);
					if((accelometerVsName != null) || isAccelometerRecent)
						storage.updateSamplingRate(accelometerType, SamplingRateAccelometerMoving);
					if(gpsVsName != null || isgpsRecent)
						storage.updateSamplingRate(gpsType, SamplingRateGPSMoving);
					if(wifiVsName != null || iswifiRecent)
						storage.updateSamplingRate(wifiType, SamplingRateWifiMoving);
					if(activityVsName != null || isAccelometerRecent)
						storage.updateSamplingRate(activityType, SamplingRateActivityMoving);		
					
					if(priMachineState == 0) // in this case we have to turn off the wifi
					{
						String wifiVSName = StaticData.findNameVs(wifiType);
						intentWifi = StaticData.getRunningIntentByName(wifiVSName);
						if(intentWifi != null)
						{
							config = StaticData.findConfig(StaticData.retrieveIDByName(wifiVSName));
							intentWifi.putExtra("tinygsn.beans.config", config);
							
							//stopService(new Intent(this, WifiService.class));
							startService(intentWifi);
							config = null;
						}
					}
					
					
					String gpsVSName = StaticData.findNameVs(gpsType);
					intentGPS = StaticData.getRunningIntentByName(gpsVSName);

					if(intentGPS != null)
					{ 
						config = StaticData.findConfig(StaticData.retrieveIDByName(gpsVSName));
						intentGPS.putExtra("tinygsn.beans.config", config);
						startService(intentGPS);
					}
					priMachineState = 1;
				}
				
				if((gpsResult != null && gpsResult.size() != 0) && (curTime - gpsResult.get(0).getTimeStamp() > timeThereshold)) //gps lost
				{
					priMachineState = 1;
					machineState = 0;
				}
				else if((gpsResult != null && gpsResult.size() != 0 && (curTime - gpsResult.get(0).getTimeStamp() < timeThereshold) && gpsConstant ) || (accelometerResult != null && accelometerResult.size() != 0 && (curTime - accelometerResult.get(0).getTimeStamp() < timeThereshold ) && ((avgXChangedAccelometer < accelometerThereshold) && (avgYChangedAccelometer < accelometerThereshold) && (avgZChangedAccelometer < accelometerThereshold)) ) )
				{
					//Accelometer low || gps constant --> stationary
					priMachineState = 1;
					machineState = 2;
				}
				break;
			case 2:
				if(priMachineState != 2 || isAccelometerRecent || isActivityRecent || isGyroscopeRecent || isgpsRecent || iswifiRecent)
				{
					if(gyroscopeVsName != null || isGyroscopeRecent)
						storage.updateSamplingRate(gyroscopeType, SamplingRateGyroscopeStationary);
					if(accelometerVsName != null || isAccelometerRecent)
						storage.updateSamplingRate(accelometerType,SamplingRateAccelometerStationary);
					if(gpsVsName != null || isgpsRecent)
						storage.updateSamplingRate(gpsType, SamplingRateGPSStationary);
					if(wifiVsName != null || iswifiRecent)
						storage.updateSamplingRate(wifiType, SamplingRateWifiStationary);
					if(activityVsName != null || isAccelometerRecent)
						storage.updateSamplingRate(activityType, SamplingRateActivityStationary);
					
					
					if(priMachineState == 1) // in this case we have to turn on the wifi and activity
					{
						String wifiVSName = StaticData.findNameVs(wifiType);
						intentWifi = StaticData.getRunningIntentByName(wifiVSName);
						if(intentWifi != null)
						{
							config = StaticData.findConfig(StaticData.retrieveIDByName(wifiVSName));
							
							intentWifi.putExtra("tinygsn.beans.config", config);
							startService(intentWifi);
							config = null;
						}
						
						String activityVSName = StaticData.findNameVs(activityType);
						intentActivity = StaticData.getRunningIntentByName(activityVSName);
						if(intentActivity != null)
						{
							config = StaticData.findConfig(StaticData.retrieveIDByName(activityVSName));
							intentActivity.putExtra("tinygsn.beans.config", config);
							startService(intentActivity);
							config = null;
						}
					}
					else if (priMachineState == 0) //in this case we only need to turn on the activity sensor
					{
					
						String activityVSName = StaticData.findNameVs(activityType);
						intentActivity = StaticData.getRunningIntentByName(activityVSName);
						if(intentActivity != null)
						{
							config = StaticData.findConfig(StaticData.retrieveIDByName(activityVSName));
							intentActivity.putExtra("tinygsn.beans.config", config);
							startService(intentActivity);
							config = null;
						}
					
					}
			
					//starting GPS 
					String gpsVSName = StaticData.findNameVs(gpsType);
					intentGPS = StaticData.getRunningIntentByName(gpsVSName);
					if(intentGPS != null)
					{

						config = StaticData.findConfig(StaticData.retrieveIDByName(gpsVSName));
						intentGPS.putExtra("tinygsn.beans.config", config);
						startService(intentGPS);	

						bindService(intentGPS, null, 0);
					}
					
					priMachineState = 2;

					if((wifiResult != null && wifiResult.size() != 0) 
							&& (((curTime - wifiResult.get(0).getTimeStamp() < timeThereshold)
									&& !isInKnownWifiAccess) || 
									(curTime - wifiResult.get(0).getTimeStamp() > timeThereshold))
									|| (accelometerResult != null && accelometerResult.size() != 0)
									&&(curTime - accelometerResult.get(0).getTimeStamp() < timeThereshold) &&
									((avgXChangedAccelometer > accelometerThereshold) || (avgYChangedAccelometer > accelometerThereshold) || (avgZChangedAccelometer > accelometerThereshold)))
					{
						priMachineState = 2;
						machineState = 0;
					}
				}
		}
//		priMachineState = -1;
//		machineState = (machineState+1)%3;
		return null;
		
	}
	
	private boolean ContainsFamiliarWifis(ArrayList<StreamElement> wifiResult,
			long curTime) {
		
		Map<String, Integer> freqs = storage.getFrequencies();
		int numOfFamiliarplaces = freqs.size()/10;
		if(numOfFamiliarplaces == 0 && freqs.size() > 0)
			numOfFamiliarplaces = 1;
		ArrayList<Integer> frequency = new ArrayList<Integer>();
		ArrayList<String> macAdr = new ArrayList<String>();
		for(Entry<String, Integer> item: freqs.entrySet())
		{
			macAdr.add(item.getKey());
			frequency.add(item.getValue());
		}
		for(int i = 0; i < macAdr.size(); i++)
		{
			for (int j = i; j < macAdr.size()-1; j++)
			{
				if(frequency.get(j) > frequency.get(j+1))
				{
					int temp = frequency.get(j);
					frequency.set(j, frequency.get(j+1));
					frequency.set(j+1, temp);
					
					String tempAdr = macAdr.get(j);
					macAdr.set(j, macAdr.get(j+1));
					macAdr.set(j+1, tempAdr);
				}
			}
		}
		for(int i = 0; i < wifiResult.size(); i++)
		{
			for(int j = 0; j < numOfFamiliarplaces; j++)
			{
				if( converTingBSSID(macAdr.get(j).substring(0, 8)) == Double.parseDouble(wifiResult.get(i).getData("mac1").toString()) && converTingBSSID(macAdr.get(j).substring(8)) == Double.parseDouble(wifiResult.get(i).getData("mac2").toString()) )
				{
					return true;
				}
			}
		}
		int j = numOfFamiliarplaces;
		if(numOfFamiliarplaces != 0)
		while(j < frequency.size() && frequency.get(j) == frequency.get(j-1))
		{
			for(int i = 0; i < wifiResult.size(); i++)
			{
				if( converTingBSSID(macAdr.get(j).substring(0, 8)) == Double.parseDouble(wifiResult.get(i).getData("mac1").toString()) && converTingBSSID(macAdr.get(j).substring(8)) == Double.parseDouble(wifiResult.get(i).getData("mac2").toString()) )
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private double converTingBSSID(String bssid)
	{
		bssid = bssid.replaceAll(":", "");
		return Integer.parseInt(bssid, 16);
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
