/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: src/ch/epfl/gsn/http/rest/DistributionRequest.java
*
* @author Ali Salehi
* @author Timotee Maret
* @author Julien Eberle
*
*/

package ch.epfl.gsn.delivery;

import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.utils.models.AbstractModel;

public interface DistributionRequest {

	public abstract boolean deliverStreamElement(StreamElement se);

    public boolean deliverKeepAliveMessage();

    public abstract long getStartTime();

    public abstract long getLastVisitedPk();

    public abstract String getQuery();

    public abstract VSensorConfig getVSensorConfig();

    public abstract void close();

    public abstract boolean isClosed();

    public abstract DeliverySystem getDeliverySystem();
    
    public abstract AbstractModel getModel();

}