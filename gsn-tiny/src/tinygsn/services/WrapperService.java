package tinygsn.services;

import tinygsn.beans.StaticData;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.wrappers.AbstractWrapper;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

public abstract class WrapperService extends IntentService{

	public WrapperConfig config = null;
	public AbstractWrapper w;
	
	public WrapperService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getExtras();
		config = (WrapperConfig) b.get("tinygsn.beans.config");
		if (!config.isRunning()) return;
		try {
			w = StaticData.getWrapperByName(config.getWrapperName());
			w.runOnce();
		} catch (Exception e1) {}
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+1000*w.getDcInterval(),PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT));
	}
	
}
