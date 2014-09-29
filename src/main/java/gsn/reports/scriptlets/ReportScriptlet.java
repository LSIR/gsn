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
* File: src/gsn/reports/scriptlets/ReportScriptlet.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package gsn.reports.scriptlets;

import gsn.reports.beans.VirtualSensor;

import java.util.Collection;
import java.util.Iterator;
import java.util.TimeZone;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;

public class ReportScriptlet extends JRDefaultScriptlet {
	
	public ReportScriptlet () {
		super () ;
	}
	
	public void afterReportInit() throws JRScriptletException {
		setListOfVirtualSensors () ;
		setServerTimeZone () ;
	}
	
	private void setServerTimeZone () throws JRScriptletException {
		this.setVariableValue("serverTimeZone", 
				TimeZone.getDefault().getDisplayName().toString() + 
				" - " +
				TimeZone.getDefault().getID().toString()
		);
	}
	
	@SuppressWarnings("unchecked")
	private void setListOfVirtualSensors () throws JRScriptletException {
		Collection<VirtualSensor> virtualSensors = (Collection<VirtualSensor>) this.getFieldValue("virtualSensors");
		StringBuilder sb = new StringBuilder () ;
		Iterator iter = (Iterator) virtualSensors.iterator();
		String nextName;
		while (iter.hasNext()) {
			nextName = ((VirtualSensor)iter.next()).getVirtualSensorName();
			sb.append(nextName);
			if (iter.hasNext()) sb.append(", ") ;
		}
		this.setVariableValue("listOfVirtualSensors", sb.toString());
	}	
}
