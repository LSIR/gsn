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
* File: src/gsn/utils/protocols/EPuck/Reset.java
*
* @author Jerome Rousselot
* @author Ali Salehi
*
*/

package gsn.utils.protocols.EPuck;

import gsn.utils.protocols.AbstractHCIQueryWithoutAnswer;

import java.util.Vector;

public class Reset extends AbstractHCIQueryWithoutAnswer {

	public static final String queryDescription = "Resets the state of the EPuck robot.";
	public static final String[] paramsDescriptions = null;
	public Reset (String name) {
		super(name, queryDescription, paramsDescriptions);
	}


	/*
	 * This query does not take any parameters.
	 * If you provide any, these will be ignored.
	 */
	public byte [ ] buildRawQuery ( Vector < Object > params ) {
		byte[] query = new byte[1];
		query[0] = 'r';
		return query;
	}
}
