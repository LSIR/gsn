package tinygsn.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.model.publishers.AbstractDataPublisher;

public class PublisherService extends IntentService {

	public DeliveryRequest dr = null;
	public AbstractDataPublisher adp;

	public PublisherService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle b = intent.getBundleExtra("tinygsn.beans.dr");
		dr = b.getParcelable("tinygsn.beans.dr");
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		if (!dr.isActive()) {
			am.cancel(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
			return;
		}
		try {
			adp = StaticData.getDataPublisherByID(dr.getId());
			adp.runOnce();
		} catch (Exception e1) {
			Log.e("PublisherService", e1.getMessage());
		}
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + dr.getIterationTime(), PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
	}
}

