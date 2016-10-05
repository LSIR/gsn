/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2015, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/publishers/AbstractDataPublisher.java
 *
 * @author Schaer Marc
 */
package tinygsn.model.subscribers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.epfl.locationprivacy.util.Utils;

import tinygsn.beans.StaticData;
import tinygsn.beans.Subscription;
import tinygsn.controller.AndroidControllerSubscribe;
import tinygsn.services.SubscriberService;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;


public abstract class AbstractSubscriber {

	private final static String LOGTAG = "AbstractSubscriber";
	protected SqliteStorageManager storage;
	protected Subscription su;
    protected AndroidControllerSubscribe controller;

	public AbstractSubscriber(Subscription su) {
        controller = new AndroidControllerSubscribe();
		storage = new SqliteStorageManager();
		this.su = su;
	}

    public static boolean startService() {
        try {
            Intent serviceIntent = new Intent(StaticData.globalContext, SubscriberService.class);
            StaticData.globalContext.startService(serviceIntent);
            return true;
        } catch (Exception e) {
            // release anything?
        }
        return false;
    }

	public abstract void retrieve(long until);

    public static AbstractSubscriber getSubscriber(Subscription su){
        switch (su.getMode()) {
            case 0:
                return new PeriodicGSNAPISubscriber(su);
            case 1:
                return new OpportunisticGSNAPISubscriber(su);
            case 2:
                return new GCMSubscriber(su);
        }
        return null;
    }

	protected static void log(Context context, String s) {
		/*if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Subscribe");
			Logging.appendLog("Subscribe", LOGTAG + ".txt", s, context);
		}*/
	}

    public abstract long getNextRun();

    public abstract void runOnce();
}
