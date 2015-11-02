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
* File: gsn-tiny/src/tinygsn/gui/android/utils/VSRow.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android.utils;

import java.io.Serializable;

/**
 * A Virtual Sensor row in the List of VS activity  
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 *
 */
public class VSRow implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8585186135377493021L;
	private String name;
	private boolean isRunning;
	private String latestValue;
	
	
	public VSRow(String name, boolean isRunning, String latestValues) {
		super();
		this.setName(name);
		this.setRunning(isRunning);
		this.setLatestValue(latestValues);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	public String getLatestValue() {
		return latestValue;
	}

	public void setLatestValue(String latestValue) {
		this.latestValue = latestValue;
	}

}
