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
 * File: gsn-tiny/src/tinygsn/model/publishers/PeriodicDataPublisher.java
 *
 * @author Schaer Marc
 */

package tinygsn.model.publishers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Locale;

import gsn.http.rest.PushDelivery;
import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.publishers.utils.PublishDataTask;
import tinygsn.services.PublisherService;


public class PeriodicDataPublisher extends AbstractDataPublisher {

	private final static String LOGTAG = "PeriodicDataPublisher";

	public PeriodicDataPublisher(DeliveryRequest dr) {
		super(dr);
	}

	public final Class<? extends PublisherService> getSERVICE() {
		return PeriodicallyDataPublisherService.class;
	}

	@Override
	public void publish(DeliveryRequest dr) {
		long currentTime = System.currentTimeMillis();
		long lastTime = dr.getLastTime();
		SimpleDateFormat formatDay = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
		SimpleDateFormat formatHour = new SimpleDateFormat("HH.mm", Locale.ENGLISH);
		String fromDate = formatDay.format(lastTime);
		String toDate = formatDay.format(currentTime);
		String fromTime = formatHour.format(lastTime);
		String toTime = formatHour.format(currentTime);
		StreamElement[] se = controller.loadRangeData(dr.getVsname(), fromDate, fromTime, toDate, toTime);
		PushDelivery pd = new PushDelivery(dr.getUrl() + "/streaming/", Double.parseDouble(dr.getKey()));
		if (se.length > 0) {
			ConnectivityManager connManager = (ConnectivityManager) StaticData.globalContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connManager.getActiveNetworkInfo() == null) {
				log(StaticData.globalContext, "No Network for uploading data");
			} else {
				try {
					new PublishDataTask(pd, this, dr).execute(se).get();
				} catch (Exception e) {
					Log.e(LOGTAG, e.getMessage());
					e.printStackTrace();
				}
			}
		} else {
			log(StaticData.globalContext, "No data to publish");
		}
	}

	@Override
	public void runOnce() {
		publish(dr);
	}

	public static class PeriodicallyDataPublisherService extends PublisherService {

		public PeriodicallyDataPublisherService() {
			super("periodicallyDataPublisherService");

		}
	}
}
