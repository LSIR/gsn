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
* File: src/gsn/reports/beans/Data.java
*
* @author Timotee Maret
*
*/

package gsn.reports.beans;

public class Data {
	
	private Object p1;
	
	private Object p2;
	
	private Number value;
	
	private String label;

	public Data (Object p1, Object p2, Number value, String label) {
		this.p1 = p1;
		this.p2 = p2;
		this.value = value;
		this.label = label;
	}

	public Object getP1() {
		return p1;
	}

	public Object getP2() {
		return p2;
	}

	public Number getValue() {
		return value;
	}

	public String getLabel() {
		return label;
	}

}
