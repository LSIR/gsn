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
 * File: gsn-tiny/src/tinygsn/model/publishers/OnDemandDataPublisher.java
 *
 * @author Schaer Marc
 */
package tinygsn.model.publishers;

import android.content.Context;
import android.net.ConnectivityManager;

import java.util.List;

import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.utils.Oauth2Connection;
import tinygsn.model.publishers.utils.PublishDataTask;
import tinygsn.storage.db.SqliteStorageManager;


public class OnDemandDataPublisher extends AbstractDataPublisher {

	public OnDemandDataPublisher(DeliveryRequest dr) {
		super(dr);
	}

	@Override
	public long getNextRun() {
		return Long.MAX_VALUE;
	}

	@Override
	public void publish(long until) {
		try {
			SqliteStorageManager storage = new SqliteStorageManager();
			List<StreamElement> se = storage.executeQueryGetRangeData("vs_" + dr.getVsname(), dr.getLastTime(), until);
			Oauth2Connection c = new Oauth2Connection(dr.getUrl(), dr.getClientID(), dr.getClientSecret());
			if (se.size() > 0) {
				ConnectivityManager connManager = (ConnectivityManager) StaticData.globalContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (connManager.getActiveNetworkInfo() == null) {
					log(StaticData.globalContext, "No Network for uploading data");
				} else {
					new PublishDataTask(c, dr).execute(se);
				}
			} else {
				log(StaticData.globalContext, "No data to publish");
			}
		}catch (Exception e){

		}
	}

	@Override
	public void runOnce() {
		// Do nothing
	}

}
