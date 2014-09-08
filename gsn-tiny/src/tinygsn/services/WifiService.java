package tinygsn.services;


import java.io.Serializable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tinygsn.beans.InputStream;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AndroidControllerListVSNew;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.wrappers.WifiWrapper;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class WifiService extends IntentService {
	
	private VSensorConfig config = null;
	public AbstractWrapper w;
	WifiManager mainWifiObj;
	WifiScanReceiver wifiReciever;
	
	public WifiService(String name)
	{
		super(name);
	}
	
	public WifiService()
	{
		super("wifiService");
	}
	
	@Override
	public int onStartCommand(Intent intent,int flags, int startId) {

		Bundle b = intent.getExtras();
		Log.d("in service", "on handle intent" + intent);
		config = (VSensorConfig) b.get("tinygsn.beans.config");
		for (InputStream inputStream : config.getInputStreams()) {
			for (StreamSource streamSource : inputStream.getSources()) {
				w = streamSource.getWrapper();
			}
		}
		Timer timer = new Timer(); 
		  // mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		 //  wifiReciever = new WifiScanReceiver();
		 //  wifiReciever.setMainWifiObj(mainWifiObj);
			//Log.i("OnHandle","OnHandle");
		//   mainWifiObj.startScan();
		   
		   // Initiate wifi service manager
		registerReciever();
		 timer.scheduleAtFixedRate( new TimerTask() {
		
			 public void run() {

			 //Do whatever you want to do every “INTERVAL”
					mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			        
				       // Check for wifi is disabled
				       if (mainWifiObj.isWifiEnabled() == false)
				            {  
				                // If wifi disabled then enable it
				                 
				    	   	mainWifiObj.setWifiEnabled(true);
				            }
				        
				       // wifi scaned value broadcast receiver
				       //wifiReciever = new WifiScanReceiver();
				        
				       // Register broadcast receiver
				       // Broacast receiver will automatically call when number of wifi connections changed
				       Log.i("OnHandle","OnHandle");
				      // registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				       mainWifiObj.startScan();
			 }

			 }, 0, 100000);
		return startId;
	}


//		 while(true)
//		 {
			
//			 try {
//				Thread.sleep(100000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			 
	//			 }
//		return startId;
	//}
	public void registerReciever()
	{
//		mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        
//	       // Check for wifi is disabled
//	       if (mainWifiObj.isWifiEnabled() == false)
//	            {  
//	                // If wifi disabled then enable it
//	                 
//	    	   	mainWifiObj.setWifiEnabled(true);
//	            }
	        
	       // wifi scaned value broadcast receiver
	      wifiReciever = new WifiScanReceiver();
	        
	       // Register broadcast receiver
	       // Broacast receiver will automatically call when number of wifi connections changed
	      // Log.i("OnHandle","OnHandle");
	       registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	    //   mainWifiObj.startScan();
	
	}
		
	
	@Override
	public void onDestroy() {
		Log.i("ondest","onDest");
	    unregisterReceiver(wifiReciever);
		super.onDestroy();
	}


	class WifiScanReceiver extends BroadcastReceiver {
		   String wifis[];
   
	   @SuppressLint("UseValueOf")
	   public void onReceive(Context c, Intent intent) {
	   	Log.i("OnReceive","OnReceive");
	      List<ScanResult> wifiScanList = mainWifiObj.getScanResults();
	      wifis = new String[wifiScanList.size()];
	      StreamElement streamElement = null;
	      for(int i = 0; i < wifiScanList.size(); i++){
	         wifis[i] = ((wifiScanList.get(i)).toString());
	         Log.i("BSSID", wifiScanList.get(0).BSSID);
	         	streamElement = new StreamElement(w.getFieldList(), w.getFieldType(),
	 				new Serializable[] { converTingBSSID(wifiScanList.get(i).BSSID.substring(0, 8)), converTingBSSID(wifiScanList.get(i).BSSID.substring(8)), wifiScanList.get(i).level });
	         ((WifiWrapper) w).getQueuedStreamElements().add(streamElement);
	      }
	      for(int i = 0; i < ((WifiWrapper) w).getQueuedStreamElements().size(); i++)
	      {
	    	  ((WifiWrapper) w).setTheLastStreamElement(((WifiWrapper) w).getQueuedStreamElements().get(i));
	  		((WifiWrapper) w).getLastKnownData();
	      }
	   }
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		
	}
	private double converTingBSSID(String bssid)
	{
		bssid = bssid.replaceAll(":", "");
		Log.i("bssid",bssid);
		return Integer.parseInt(bssid, 16);
	}
	
}