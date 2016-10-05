/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/wrappers/AndroidGyroscopeWrapper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;


public class WifiWrapper extends AbstractWrapper {
	
	public WifiWrapper(WrapperConfig wc) {
		super(wc);
	}
	public WifiWrapper() {
	}

	private static final String[] FIELD_NAMES = new String[] { "mac1", "mac2", "level" };
	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE };
	private static final String[] FIELD_DESCRIPTION = new String[] { "mac1", "mac2", "level" };
	private static final String[] FIELD_TYPES_STRING = new String[] { "double", "double", "double" };
	
	public final Class<? extends WrapperService> getSERVICE(){ return WifiService.class;}
	
	@Override
	public void runOnce(){}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[output.size()]);
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}
	
	public static class WifiService extends WrapperService {
		
		WifiManager mainWifiObj;
		WifiScanReceiver wifiReciever;
		SqliteStorageManager storage = null;
		long ctr = 0;
		boolean scanning = false;
		boolean wifiEnabled = false;
		
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
			Bundle b = intent.getBundleExtra("tinygsn.beans.config");
			config = (WrapperConfig) b.getParcelable("tinygsn.beans.config");
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			if (!config.isRunning()){
				am.cancel(PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
				return;
			}
			storage = new SqliteStorageManager();
			try {
				w = StaticData.getWrapperByName("tinygsn.model.wrappers.WifiWrapper");
			} catch (Exception e1) {}
			
			int duration = w.getDcDuration();
			int interval = w.getDcInterval();
				
		    if (duration > 0 && ctr % duration == 0 ){
		    	
		    	wifiEnabled = mainWifiObj.isWifiEnabled();
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
				//return wifi to its previous state
				mainWifiObj.setWifiEnabled(wifiEnabled);

			}
		    ctr++;

			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+1000*interval,PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
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

}