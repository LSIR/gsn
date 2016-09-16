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
* File: src/ch/epfl/gsn/utils/protocols/EPuck/BodyLED.java
*
* @author Jerome Rousselot
* @author Ali Salehi
*
*/

package ch.epfl.gsn.utils.protocols.EPuck;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

import ch.epfl.gsn.utils.protocols.AbstractHCIQueryWithoutAnswer;

public class BodyLED extends AbstractHCIQueryWithoutAnswer {

	public static final String queryDescription = "Switches the body LED on and off.";
	public static final String[] paramsDescriptions = null;
	
	public enum LED_STATE {OFF, ON, INVERSE};
	/**
	 * @param Name
	 * @param queryDescription
	 * @param paramsDescriptions
	 */
	public BodyLED(String Name) {
		super(Name, queryDescription, paramsDescriptions);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see ch.epfl.gsn.utils.protocols.AbstractHCIQuery#buildRawQuery(java.util.Vector)
	 */
	@Override
	public byte[] buildRawQuery(Vector<Object> params) {
		byte[] answer = null;
		if(params.firstElement() != null && params.firstElement() instanceof Integer) {
			answer = new byte[3];
			answer[0] = 'B';
			answer[1] = ',';
			try {
				answer[2] = ((Integer) params.firstElement()).toString().getBytes("UTF-8")[0];
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// TODO Auto-generated method stub
		return answer;
	}

}
