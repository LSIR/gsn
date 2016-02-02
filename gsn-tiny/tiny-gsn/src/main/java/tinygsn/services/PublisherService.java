package tinygsn.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import tinygsn.beans.DeliveryRequest;
import tinygsn.model.publishers.AbstractDataPublisher;
import tinygsn.storage.db.SqliteStorageManager;

public class PublisherService extends IntentService {

	public PublisherService() {
		super("tinyGSN publisher");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        SqliteStorageManager storage = new SqliteStorageManager();
        long next_run = 10 * 60 * 1000L; //at least once every 10 minutes
        try {
            for (DeliveryRequest dr : storage.getPublishList()) {
                try {
                    if (dr.isActive()) {
                        AbstractDataPublisher adp = AbstractDataPublisher.getPublisher(dr);
                        adp.runOnce();
                        next_run = Math.min(next_run, adp.getNextRun());
                    }
                } catch (Exception e1) {
                    Log.e("PublisherService", e1.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("PublisherService", e.getMessage());
        }
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + next_run, PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
	}
}

