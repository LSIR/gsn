package tinygsn.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityListVSNew;
import tinygsn.model.wrappers.AndroidAccelerometerWrapper;
import tinygsn.storage.db.SqliteStorageManager;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class schedular extends IntentService {

	public schedular(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	public schedular()
	{
		super("schedulat");
	}

	Context context = null; //TODO set this parameter
	int machineState = 0; //0 = lost, 1= moving 2= stationary
	int priMachineState = -1;
	SqliteStorageManager storage = new SqliteStorageManager(new ActivityListVSNew());
	private int numLatest = 10;
	
	double accelometerThereshold = 1;
	AndroidAccelerometerWrapper accelometerWrapper = new AndroidAccelerometerWrapper();
	final String accelometerType = "tinygsn.model.wrappers.AndroidAccelerometerWrapper";
	
	
	
	
	public void scheduleServices(ArrayList<Integer>rates)
	{
		
	}
	public ArrayList<Integer> CalcRates()
	{
		
		
		long curTime = System.currentTimeMillis();
		double avgXChanged = 0;
		double avgYChanged = 0;
		double avgZChanged = 0;
		ArrayList<StreamElement> result = null;
		if(StaticData.findNameVs(accelometerType) != null)
		{
			Log.i("scheeeeeeeedular", StaticData.findNameVs(accelometerType));
			result =  storage.executeQueryGetLatestValues("vs_" + StaticData.findNameVs(accelometerType), accelometerWrapper.getFieldList(), accelometerWrapper.getFieldType(), numLatest);
			for(int i = 0; i < result.size(); i++)
			{
				avgXChanged += Math.abs(Double.parseDouble(result.get(i).getData("x").toString()));
				avgYChanged += Math.abs(Double.parseDouble(result.get(i).getData("y").toString()));
				avgZChanged += Math.abs(Double.parseDouble(result.get(i).getData("z").toString()));
			}
			avgXChanged = avgXChanged/10;
			avgYChanged = avgYChanged/10;
			avgZChanged = avgZChanged/10;
		}	
		Log.i("machine State", machineState + "");
		switch(machineState)
		{
			
			case 0:
				if(priMachineState != 0)
				{
					priMachineState = 0;
					storage.updateSamplingRate(accelometerType,0);
				}
				//changing stage
				if((result != null) &&((curTime - result.get(0).getTimeStamp() < 1000 * 60 * 5) || ((avgXChanged < accelometerThereshold) && (avgYChanged < accelometerThereshold) && (avgZChanged < accelometerThereshold))))
				{
					Log.i("avgx", (curTime - result.get(0).getTimeStamp() < 1000 * 60 * 5)+"");
					//accelometer low --> going to stationary state
					priMachineState = 0;
					machineState = 2;
				}
				break;
			case 1:
				if(priMachineState != 1)
				{
					priMachineState = 1;
					storage.updateSamplingRate(accelometerType,1);
				}
				
				priMachineState = 1;
				machineState = 2;
							
				break;
			case 2:
				if(priMachineState != 2)
				{
					priMachineState = 2;
					storage.updateSamplingRate(accelometerType,2);
				}
				
				
				if((result != null)&&(curTime - result.get(0).getTimeStamp() < 1000 * 60 * 5) && ((avgXChanged > accelometerThereshold) || (avgYChanged > accelometerThereshold) || (avgZChanged > accelometerThereshold)))
				{
					//accelometer low --> going to stationary state
					priMachineState = 2;
					machineState = 0;
				}
				
		}
		return null;
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		while(true)
		{
			storage = new SqliteStorageManager(new ActivityListVSNew());
			Log.i("in schedular", "in schedular");
			CalcRates();
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	//	return startId;
	 
		
	}
	
	
}
