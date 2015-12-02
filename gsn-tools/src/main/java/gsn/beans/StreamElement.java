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
* File: src/gsn/beans/StreamElement.java
*
* @author rhietala
* @author Timotee Maret
* @author Sofiane Sarni
* @author Ali Salehi
* @author Mehdi Riahi
* @author Julien Eberle
*
*/

package gsn.beans;

import java.io.Serializable;


public final class StreamElement implements Serializable {

	private static final long                      serialVersionUID  = 2000261462783698617L;

	private long                                   timeStamp         = -1;

	private String [ ]                             fieldNames;

	private Serializable [ ]                       fieldValues;

	private transient Byte [ ]                               fieldTypes;

	private boolean timestampProvided = false;


	public StreamElement(){
		
	}

	public final String [ ] getFieldNames ( ) {
		return this.fieldNames;
	}

	public final Byte [ ] getFieldTypes ( ) {
		return this.fieldTypes;
	}

	public final Serializable [ ] getData ( ) {
		return this.fieldValues;
	}

	public long getTimeStamp ( ) {
		return this.timeStamp;
	}

	
}
