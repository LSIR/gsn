package tinygsn.services;

import tinygsn.beans.InputStream;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.beans.WrapperConfig;
import tinygsn.controller.AndroidControllerListVS;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.storage.db.SqliteStorageManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public abstract class WrapperService extends IntentService{

	private WrapperConfig config = null;
	public AndroidControllerListVS VSNewController;
	public AbstractWrapper w;
	
	public WrapperService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getExtras();
		config = (WrapperConfig) b.get("tinygsn.beans.config");
		if (!config.isRunning()) return;
		Activity activity = config.getController().getActivity();
		try {
			w = StaticData.getWrapperByName("tinygsn.model.wrappers.WifiWrapper");
			w.runOnce();
		} catch (Exception e1) {}
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+15000,PendingIntent.getService(activity, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
	}
	
}
