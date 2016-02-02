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


import tinygsn.beans.Subscription;

public class GCMSubscriber extends AbstractSubscriber {

	public GCMSubscriber(Subscription su) {
		super(su);
	}

	private final static String LOGTAG = "GCMSub";


	@Override
	public long getNextRun() {
		return Long.MAX_VALUE;  //or maybe use one day
	}

	@Override
	public void retrieve(long until) {
		// register with GSN server
	}

	@Override
	public void runOnce() {
		// call retrieve, but not too often
	}
}

