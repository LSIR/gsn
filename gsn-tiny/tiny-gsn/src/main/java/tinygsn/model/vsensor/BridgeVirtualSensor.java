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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/vsensor/BridgeVirtualSensor.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.vsensor;

import tinygsn.beans.StreamElement;



public class BridgeVirtualSensor extends AbstractVirtualSensor {

	private static final long serialVersionUID = -7656375392762513783L;

	@Override
	public boolean initialize() {
		return false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		StreamElement anonymizedData = super.anonymizeData(inputStreamName, streamElement);
		dataProduced(anonymizedData);
	}

}
