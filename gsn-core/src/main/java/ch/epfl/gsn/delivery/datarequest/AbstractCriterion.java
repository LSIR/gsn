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
* File: src/ch/epfl/gsn/http/datarequest/AbstractCriterion.java
*
* @author Timotee Maret
*
*/

package ch.epfl.gsn.delivery.datarequest;

import java.util.Hashtable;

import ch.epfl.gsn.delivery.datarequest.DataRequestException;

public class AbstractCriterion {
	
	protected static final String GENERAL_ERROR_MSG 	= "Failed to create the Criteria";
	protected static final String CRITERION_ERROR_MSG 	= "Invalid Criterion";
	
	public String getCriterion (String criterion, Hashtable<String, String> allowedValues) throws DataRequestException {
		if (allowedValues.containsKey(criterion.toLowerCase())) {
			return allowedValues.get(criterion.toLowerCase());
		}
		else throw new DataRequestException (CRITERION_ERROR_MSG + " >" + criterion + "<. Valid values are >" + allowedValues.keySet().toString() + "<") ;
	}
}
