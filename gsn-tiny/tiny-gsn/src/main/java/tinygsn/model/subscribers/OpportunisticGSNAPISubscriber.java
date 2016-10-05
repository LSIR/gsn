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
 * File: gsn-tiny/src/tinygsn/model/publishers/OpportunisticGSNAPISubscriber.java
 *
 * @author Schaer Marc
 */
package tinygsn.model.subscribers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import tinygsn.beans.StaticData;
import tinygsn.beans.Subscription;
import tinygsn.model.subscribers.utils.RetrieveDataTask;

public class OpportunisticGSNAPISubscriber extends AbstractSubscriber {

	public OpportunisticGSNAPISubscriber(Subscription su) {
		super(su);
	}

	private final static String LOGTAG = "OpportunisticDataSub";


	@Override
	public long getNextRun() {
		long currentTime = System.currentTimeMillis();
		if (su.getLastTime() + su.getIterationTime() > currentTime){
			return su.getLastTime() + su.getIterationTime() - currentTime;
		}else{
			// if deadline is passed, lets try at next iteration
			return su.getIterationTime() - (currentTime - su.getLastTime()) % su.getIterationTime();
		}
	}

	@Override
	public void retrieve(long until) {
		ConnectivityManager connManager = (ConnectivityManager) StaticData.globalContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (connManager.getActiveNetworkInfo() == null) {
			log(StaticData.globalContext, "No Network for downloading data");
		} else {
			if (!mWifi.isConnected()) {
				log(StaticData.globalContext, "Wifi is not connected for downloading data");
			} else {
				try {
					new RetrieveDataTask(su).execute(su.getUrl()).get();
				} catch (Exception e) {
					Log.e(LOGTAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void runOnce() {
		long currentTime = System.currentTimeMillis();
		if (su.getLastTime() + su.getIterationTime() < currentTime){
			retrieve(currentTime);
		}
	}
}

