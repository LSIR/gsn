package tinygsn.services;


import java.io.Serializable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tinygsn.beans.InputStream;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;

import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.WifiWrapper;
import tinygsn.storage.db.SqliteStorageManager;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class WifiService extends IntentService {
	
	private VSensorConfig config = null;
	public AbstractWrapper w;
	WifiManager mainWifiObj;
	WifiScanReceiver wifiReciever;
	SqliteStorageManager storage = null;
	long ctr = 0;
	boolean scanning = false;
	
	
	public WifiService(String name)
	{
		super(name);
	}
	
	public WifiService()
	{
		super("wifiService");
	}
	
	@Override
	public void onCreate(){
		mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiReciever = new WifiScanReceiver();
		registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.d("wifi-scanning", "registered");
		super.onCreate();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {

		Bundle b = intent.getExtras();
		config = (VSensorConfig) b.get("tinygsn.beans.config");
		storage = new SqliteStorageManager(config.getController().getActivity());
		VirtualSensor vs = new VirtualSensor(config, config.getController().getActivity());
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				w = streamSource.getWrapper();
			}
		}
		//while (w.isActive()) 
		{
			//try {
				int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.WifiWrapper");
			
			    if (samplingRate > 0 && ctr % samplingRate == 0 ){

					mainWifiObj.setWifiEnabled(true);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {}
					mainWifiObj.startScan();
					scanning = true;
					Log.d("wifi-scanning", "calling scan" + wifiReciever);
					long t = System.currentTimeMillis();
					while (scanning){
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {}
						if (System.currentTimeMillis() - t > 120000){
					    	  StreamElement streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
						 				new Serializable[] { 1.0, mainWifiObj.isWifiEnabled()?1.0:0.0 , 0.0 });
						         ((WifiWrapper) w).postStreamElement(streamElement);
						         break;
						}
					}
					mainWifiObj.setWifiEnabled(false);

				}
			    ctr++;
				//Thread.sleep(60*1000);
			//}
			//catch (InterruptedException e) {
			//	Log.e(e.getMessage(), e.toString());
			//}
		}
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+60000,PendingIntent.getService(config.getController().getActivity(), 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
	}
		
	
	@Override
	public void onDestroy() {
		try{
		unregisterReceiver(wifiReciever);
		Log.d("wifi-scanning", "de-registered");
		}catch( Exception e)
		{}
		super.onDestroy();
	}


	class WifiScanReceiver extends BroadcastReceiver {
   
	   @SuppressLint("UseValueOf")
	   public void onReceive(Context c, Intent intent) {
		    
	   	  List<ScanResult> wifiScanList = mainWifiObj.getScanResults();
	   	  Log.d("wifi-scanning", "received " + wifiScanList.size());
	      StreamElement streamElement = null;
	      
	      if (wifiScanList.size() == 0){
	    	  
	    	  streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
		 				new Serializable[] { 0.0, mainWifiObj.isWifiEnabled()?1.0:0.0 , 0.0 });
		         ((WifiWrapper) w).postStreamElement(streamElement);
	      }
	      
	      for(int i = 0; i < wifiScanList.size(); i++){
	         	streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
	 				new Serializable[] { converTingBSSID(wifiScanList.get(i).BSSID.substring(0, 8)), converTingBSSID(wifiScanList.get(i).BSSID.substring(8)), wifiScanList.get(i).level });
	         ((WifiWrapper) w).postStreamElement(streamElement);
	         storage.updateWifiFrequency(wifiScanList.get(i).BSSID);
	      }
	     // unregisterReceiver(wifiReciever);
	     // registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	      scanning = false;
	   }
	}


	private double converTingBSSID(String bssid)
	{
		bssid = bssid.replaceAll(":", "");
		return Integer.parseInt(bssid, 16);
	}
	
}