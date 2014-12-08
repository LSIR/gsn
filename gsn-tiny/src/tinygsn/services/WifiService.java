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
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;


public class WifiService extends IntentService {
	
	private VSensorConfig config = null;
	public AbstractWrapper w;
	WifiManager mainWifiObj;
	WifiScanReceiver wifiReciever;
	SqliteStorageManager storage = null;
	long ctr = 0;
	
	public WifiService(String name)
	{
		super(name);
	}
	
	public WifiService()
	{
		super("wifiService");
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
		Timer timer = new Timer(); 
		registerReciever();
		mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		timer.scheduleAtFixedRate( new TimerTask() {

			public void run() {
				
				int samplingRate = storage.getSamplingRateByName("tinygsn.model.wrappers.WifiWrapper");
				if (samplingRate > 0 && ctr % samplingRate == 0 ){
					if (mainWifiObj.isWifiEnabled() == false)
					{  
						// If wifi disabled then enable it
						mainWifiObj.setWifiEnabled(true);
					}
					registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					mainWifiObj.startScan();
				}
				else
				{
					//mainWifiObj.setWifiEnabled(false);
				}
				ctr++;
			}
		}, 0, 1000*60);
	}

	public void registerReciever()
	{
		wifiReciever = new WifiScanReceiver();     
	    registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}
		
	
	@Override
	public void onDestroy() {
		unregisterReceiver(wifiReciever);
		super.onDestroy();
	}


	class WifiScanReceiver extends BroadcastReceiver {
		   String wifis[];
   
	   @SuppressLint("UseValueOf")
	   public void onReceive(Context c, Intent intent) {
		    
	   	  List<ScanResult> wifiScanList = mainWifiObj.getScanResults();
	      wifis = new String[wifiScanList.size()];
	      StreamElement streamElement = null;
	      for(int i = 0; i < wifiScanList.size(); i++){
	         wifis[i] = ((wifiScanList.get(i)).toString());
	         	streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
	 				new Serializable[] { converTingBSSID(wifiScanList.get(i).BSSID.substring(0, 8)), converTingBSSID(wifiScanList.get(i).BSSID.substring(8)), wifiScanList.get(i).level });
	         ((WifiWrapper) w).getQueuedStreamElements().add(streamElement);
	         storage.updateWifiFrequency(wifiScanList.get(i).BSSID);
	      }
	      for(int i = 0; i < ((WifiWrapper) w).getQueuedStreamElements().size(); i++)
	      {
	    	  ((WifiWrapper) w).setTheLastStreamElement(((WifiWrapper) w).getQueuedStreamElements().get(i));
	  		((WifiWrapper) w).getLastKnownData();
	      }
	      ((WifiWrapper) w).getQueuedStreamElements().clear();
	      
	   }
	}


	private double converTingBSSID(String bssid)
	{
		bssid = bssid.replaceAll(":", "");
	//	Log.i("bssid",bssid);
		return Integer.parseInt(bssid, 16);
	}
	
}