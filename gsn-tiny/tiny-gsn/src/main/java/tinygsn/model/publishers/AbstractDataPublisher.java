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
package tinygsn.model.publishers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.epfl.locationprivacy.util.Utils;

import java.net.MalformedURLException;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.controller.AndroidControllerPublish;
import tinygsn.services.PublisherService;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Logging;
import tinygsn.utils.ToastUtils;


public abstract class AbstractDataPublisher {

	private final static String LOGTAG = "AbstractDataPublisher";
	protected SqliteStorageManager storage;
	protected DeliveryRequest dr;
    protected  AndroidControllerPublish controller;

	public AbstractDataPublisher(DeliveryRequest dr) {
        controller = new AndroidControllerPublish();
		storage = new SqliteStorageManager();
		this.dr = dr;
	}

    public static boolean startService() {
        try {
            Intent serviceIntent = new Intent(StaticData.globalContext, PublisherService.class);
            StaticData.globalContext.startService(serviceIntent);
            return true;
        } catch (Exception e) {
            // release anything?
        }
        return false;
    }

	public abstract void publish(long until) throws MalformedURLException;

    public static AbstractDataPublisher getPublisher(DeliveryRequest dr){
        switch (dr.getMode()) {
            case 0:
                return new OnDemandDataPublisher(dr);
            case 1:
                return new PeriodicDataPublisher(dr);
            case 2:
                return new OpportunisticDataPublisher(dr);
        }
        return null;
    }

	protected static void log(Context context, String s) {
		/*if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Logging.createNewLoggingFolder(context, "Publish");
			Logging.appendLog("Publish", LOGTAG + ".txt", s, context);
		}*/
	}

    public abstract long getNextRun();

    public abstract void runOnce();
}
