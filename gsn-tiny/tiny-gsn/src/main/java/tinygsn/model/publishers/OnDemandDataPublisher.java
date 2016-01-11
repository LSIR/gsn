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
import android.widget.Toast;

import gsn.http.rest.PushDelivery;
import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.publishers.utils.PublishDataTask;
import tinygsn.services.PublisherService;

public class OnDemandDataPublisher extends AbstractDataPublisher {

	private String fromDate = "";
	private String fromTime = "";
	private String toDate = "";
	private String toTime = "";

	public final Class<? extends PublisherService> getSERVICE() {
		return OnDemandDataPublisherService.class;
	}

	public OnDemandDataPublisher(String fromDate, String fromTime, String toDate, String toTime, DeliveryRequest dr) {
		super(dr);
		this.fromDate = fromDate;
		this.fromTime = fromTime;
		this.toDate = toDate;
		this.toTime = toTime;
	}

	public OnDemandDataPublisher(DeliveryRequest dr) {
		super(dr);
	}

	@Override
	public void publish(DeliveryRequest dr) {
		if (fromDate == "" || fromTime == "" || toDate == "" || toTime == "") {
			Toast.makeText(StaticData.globalContext, "Please use the 'Publish' button and set correctly dates and times", Toast.LENGTH_SHORT).show();
		} else {
			StreamElement[] se = controller.loadRangeData(dr.getVsname(), fromDate, fromTime, toDate, toTime);
			PushDelivery pd = new PushDelivery(dr.getUrl() + "/streaming/", Double.parseDouble(dr.getKey()));
			if (se.length > 0) {
				ConnectivityManager connManager = (ConnectivityManager) StaticData.globalContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (connManager.getActiveNetworkInfo() == null) {
					log(StaticData.globalContext, "No Network for uploading data");
				} else {
					new PublishDataTask(pd, this, dr).execute(se);
				}
			} else {
				log(StaticData.globalContext, "No data to publish");
			}
		}
	}

	@Override
	public void runOnce() {
		// Do nothing
	}

	public static class OnDemandDataPublisherService extends PublisherService {

		public OnDemandDataPublisherService() {
			super("onDemandDataPublisherService");

		}
	}
}
