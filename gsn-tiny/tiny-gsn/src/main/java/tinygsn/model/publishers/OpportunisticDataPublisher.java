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
package tinygsn.model.publishers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.List;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.publishers.utils.PublishDataTask;
import tinygsn.model.utils.Oauth2Connection;
import tinygsn.storage.db.SqliteStorageManager;

public class OpportunisticDataPublisher extends AbstractDataPublisher {

	public OpportunisticDataPublisher(DeliveryRequest dr) {
		super(dr);
	}

	private final static String LOGTAG = "OpportunisticDataPublis";


	@Override
	public long getNextRun() {
		long currentTime = System.currentTimeMillis();
		if (dr.getLastTime() + dr.getIterationTime() > currentTime){
			return dr.getLastTime() + dr.getIterationTime() - currentTime;
		}else{
			// if deadline is passed, lets try at next iteration
			return dr.getIterationTime() - (currentTime - dr.getLastTime()) % dr.getIterationTime();
		}
	}

	@Override
	public void publish(long until) {
		try {
			SqliteStorageManager storage = new SqliteStorageManager();
			List<StreamElement> se = storage.executeQueryGetRangeData("vs_" + dr.getVsname(), dr.getLastTime(), until);
			Oauth2Connection c = new Oauth2Connection(dr.getUrl(), dr.getClientID(), dr.getClientSecret());
			if (se.size() > 0) {
				ConnectivityManager connManager = (ConnectivityManager) StaticData.globalContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (connManager.getActiveNetworkInfo() == null) {
					log(StaticData.globalContext, "No Network for uploading data");
				} else {
					if (!mWifi.isConnected()) {
						log(StaticData.globalContext, "Wifi is not connected for uploading data");
					} else {
						try {
							new PublishDataTask(c, dr).execute(se).get();
						} catch (Exception e) {
							Log.e(LOGTAG, e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
		}catch (Exception e){

		}
	}

	@Override
	public void runOnce() {
		long currentTime = System.currentTimeMillis();
		if (dr.getLastTime() + dr.getIterationTime() < currentTime){
			publish(currentTime);
		}
	}
}

