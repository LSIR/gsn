package tinygsn.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.Subscription;
import tinygsn.model.publishers.AbstractDataPublisher;
import tinygsn.model.subscribers.AbstractSubscriber;
import tinygsn.storage.db.SqliteStorageManager;

public class SubscriberService extends IntentService {

	public SubscriberService() {
		super("tinyGSN subscriber");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        SqliteStorageManager storage = new SqliteStorageManager();
        long next_run = 10 * 60 * 1000L; //at least once every 10 minutes
        try{
            for (Subscription su : storage.getSubscribeList()) {
                try {
                    if (su.isActive()) {
                        AbstractSubscriber asu = AbstractSubscriber.getSubscriber(su);
                        asu.runOnce();
                        next_run = Math.min(next_run, asu.getNextRun());
                    }
                } catch (Exception e1) {
                    Log.e("SubscriberService", e1.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("SubscriberService", e.getMessage());
        }
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + next_run, PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
    }
}

