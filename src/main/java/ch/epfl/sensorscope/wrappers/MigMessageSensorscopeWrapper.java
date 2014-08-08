/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/sensorscope/wrappers/MigMessageSensorscopeWrapper.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package ch.epfl.sensorscope.wrappers;

import gsn.acquisition2.wrappers.MigMessageParameters;
import gsn.acquisition2.wrappers.MigMessageWrapper2;

import org.apache.log4j.Logger;

public class MigMessageSensorscopeWrapper extends MigMessageWrapper2 {

	private static final int MAINTENANCE_MESSAGE_AM = 138;
	
	private long last_timestamp_offset = 0;

	private final transient Logger logger = Logger.getLogger( MigMessageSensorscopeWrapper.class );

	@Override
	public void run() {
		super.run();
		if (parameters.getTinyosVersion() == MigMessageParameters.TINYOS_VERSION_1) {
			logger.error("NOT implemented for TinyOS v 1.x");
		}
		else {
			logger.debug("Register maintenance message to source.");
			net.tinyos.message.Message messageMaintenance = (net.tinyos.message.Message) new gsn.wrappers.tinyos.SensorscopeMaintenance();
			moteIFTinyOS2x.registerListener(messageMaintenance, this);
		}
	}

	@Override
	public void messageReceived(int to, net.tinyos1x.message.Message tosmsg) {
		if (tosmsg.amType() == MAINTENANCE_MESSAGE_AM) { 
			logger.debug("TinyOS 1.x Message received");
			logger.debug("Sensorscope Maintenance message received");
			logger.error("NOT implemented for TinyOS v 1.x");
		}
		else {
			// update the timestamp in the message
			logger.debug("Sensorscope Data message received, timestamp not updated");
			super.messageReceived(to, tosmsg);
		}
	}

	@Override
	public void messageReceived(int to, net.tinyos.message.Message tosmsg) {		
		if (tosmsg.amType() == MAINTENANCE_MESSAGE_AM) { 
			logger.debug("TinyOS 2.x Message received");
			logger.debug("Sensorscope Maintenance message received");
			// update the timestamp offset
			gsn.wrappers.tinyos.SensorscopeMaintenance messageMaintenance = (gsn.wrappers.tinyos.SensorscopeMaintenance) tosmsg;
			last_timestamp_offset = messageMaintenance.get_timestamp_offset();
			logger.debug("New Sensorscope timestamp offset >" + last_timestamp_offset + "<");
		}
		else {
			logger.debug("Sensorscope Data message received");
			if (tosmsg instanceof gsn.wrappers.tinyos.RuedlingenData) {
				// update the timestamp in the message
				gsn.wrappers.tinyos.RuedlingenData m = (gsn.wrappers.tinyos.RuedlingenData) tosmsg;
				long updatedTimeStamp = last_timestamp_offset + (m.get_timestamp() / 1024);
				logger.debug("timestamp from message >" + m.get_timestamp() + "<");
				logger.debug("timestamp with offset >" + updatedTimeStamp + "<");
				m.set_timestamp(updatedTimeStamp);
			}
			else {
				logger.error("Unknow message");
			}
			super.messageReceived(to, tosmsg);
		}
	}

	@Override
	public String getWrapperName() {
		return "TinyOS Sensorscope packet wrapper";
	}

}
