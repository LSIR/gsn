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
* File: gsn-tiny/src/tinygsn/beans/InputStream.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.beans;


import java.io.Serializable;
import java.util.ArrayList;
import tinygsn.model.vsensor.AbstractVirtualSensor;


public class InputStream implements Serializable {

	private static final long serialVersionUID = 6910141410904878762L;

	//parameters
	private int rate;

	//references
	private transient ArrayList<StreamSource> sources = new ArrayList<StreamSource>();
	private transient AbstractVirtualSensor virtualSensor;
	
	
	
	public InputStream() {}
	
	public void addStreamSource(StreamSource ss){
		sources.add(ss);
	}
	
	public void removeStreamSource(StreamSource ss){
		sources.remove(ss);
	}

	public int getRate() {
		return this.rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	public ArrayList<StreamSource> getSources() {
		return sources;
	}

	public AbstractVirtualSensor getVirtualSensor() {
		return virtualSensor;
	}

	public void setVirtualSensor(AbstractVirtualSensor vs) {
		this.virtualSensor = vs;
	}
}
