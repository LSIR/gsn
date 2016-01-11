package tinygsn.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import tinygsn.beans.StaticData;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.wrappers.AbstractWrapper;

public abstract class WrapperService extends IntentService {

	public WrapperConfig config = null;
	public AbstractWrapper w;

	public WrapperService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getBundleExtra("tinygsn.beans.config");
		config = b.getParcelable("tinygsn.beans.config");
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		if (!config.isRunning()) {
			am.cancel(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
			return;
		}
		try {
			w = StaticData.getWrapperByName(config.getWrapperName());
			w.runOnce();
		} catch (Exception e1) {
		}
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * w.getDcInterval(), PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
	}
}
