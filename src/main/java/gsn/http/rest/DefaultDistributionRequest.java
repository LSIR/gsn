/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/gsn/http/rest/DefaultDistributionRequest.java
*
* @author Ali Salehi
* @author Mehdi Riahi
* @author Timotee Maret
* @author Julien Eberle
*
*/

package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.SQLValidator;
import gsn.utils.models.AbstractModel;

import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class DefaultDistributionRequest implements DistributionRequest {

	private static transient Logger       logger     = LoggerFactory.getLogger ( DefaultDistributionRequest.class );

	private long startTime;

    private long lastVisitedPk = -1;

	private String query;

	private DeliverySystem deliverySystem;

	private VSensorConfig vSensorConfig;

    private DefaultDistributionRequest(DeliverySystem deliverySystem, VSensorConfig sensorConfig, String query, long startTime) throws IOException, SQLException {
		this.deliverySystem = deliverySystem;
		vSensorConfig = sensorConfig;
		this.query = query;
		this.startTime = startTime;
		DataField[] selectedColmnNames = SQLValidator.getInstance().extractSelectColumns(query,vSensorConfig);
		deliverySystem.writeStructure(selectedColmnNames);
	}

	public static DefaultDistributionRequest create(DeliverySystem deliverySystem, VSensorConfig sensorConfig,String query, long startTime) throws IOException, SQLException {
		DefaultDistributionRequest toReturn = new DefaultDistributionRequest(deliverySystem,sensorConfig,query,startTime);
		return toReturn;
	}

	public String toString() {
		return new StringBuilder("DefaultDistributionRequest Request[[ Delivery System: ")
                .append(deliverySystem.getClass().getName())
                .append("],[Query:").append(query)
                .append("],[startTime:")
                .append(startTime)
                .append("],[VirtualSensorName:")
                .append(vSensorConfig.getName())
                .append("]]").toString();
	}

    public boolean deliverKeepAliveMessage() {
        return deliverySystem.writeKeepAliveStreamElement();
    }

	public boolean deliverStreamElement(StreamElement se) {		
		boolean success = deliverySystem.writeStreamElement(se);
//		boolean success = true;
		if (success) {
			//startTime=se.getTimeStamp();
            lastVisitedPk = se.getInternalPrimayKey();
        }
		return success;
	}


	public long getStartTime() {
		return startTime;
	}

    public long getLastVisitedPk() {
        return lastVisitedPk;
    }

	
	public String getQuery() {
		return query;
	}

	
	public VSensorConfig getVSensorConfig() {
		return vSensorConfig;
	}

	
	public void close() {
		deliverySystem.close();
	}

	
	public boolean isClosed() {
		return deliverySystem.isClosed();
	}

	public DeliverySystem getDeliverySystem() {
		return deliverySystem;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultDistributionRequest that = (DefaultDistributionRequest) o;

        if (deliverySystem != null ? !deliverySystem.equals(that.deliverySystem) : that.deliverySystem != null)
            return false;
        if (query != null ? !query.equals(that.query) : that.query != null) return false;
        if (vSensorConfig != null ? !vSensorConfig.equals(that.vSensorConfig) : that.vSensorConfig != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = query != null ? query.hashCode() : 0;
        result = 31 * result + (deliverySystem != null ? deliverySystem.hashCode() : 0);
        result = 31 * result + (vSensorConfig != null ? vSensorConfig.hashCode() : 0);
        return result;
    }

	@Override
	public AbstractModel getModel() {
		return null;
	}
}
	
