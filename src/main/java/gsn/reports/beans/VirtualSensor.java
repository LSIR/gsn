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
* File: src/gsn/reports/beans/VirtualSensor.java
*
* @author Timotee Maret
*
*/

package gsn.reports.beans;

import java.util.Collection;

public class VirtualSensor {
	
	private String virtualSensorName;
	
	private String latitude;
	
	private String longitude;
	
	private Collection<Stream> reportFields;
	
	public VirtualSensor (String virtualSensorName, String latitude, String longitude, Collection<Stream> reportFields) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.virtualSensorName = virtualSensorName;
		this.reportFields = reportFields;
	}

	public Collection<Stream> getReportFields() {
		return reportFields;
	}

	public String getVirtualSensorName() {
		return virtualSensorName;
	}

	public String getLatitude() {
		return latitude;
	}

	public String getLongitude() {
		return longitude;
	}
	
}
