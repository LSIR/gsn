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
* File: src/ch/epfl/gsn/utils/models/DummyModel.java
*
* @author Julien Eberle
*
*/

package ch.epfl.gsn.utils.models;

import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.beans.StreamElement;

/**
 * This class is just an example of implementation of an AsbtractModel.
 * It always returns the last element and if no one is set, it builds one with the default value.
 * The default value can be defined as a parameter.
 * @author jeberle
 *
 */
public class DummyModel extends AbstractModel {
	
	private StreamElement lastone;
	private int defaultValue = 0;

	@Override
	public StreamElement[] pushData(StreamElement streamElement,String origin) {
		lastone = streamElement;
		return new StreamElement[]{lastone};
	}

	@Override
	public StreamElement[] query(StreamElement params) {
		
		if (lastone != null){
			return new StreamElement[] {lastone};
		}
		else{
			return new StreamElement[]{new StreamElement(new String[]{"value"},
					new Byte[]{DataTypes.INTEGER}, new Integer[]{defaultValue})};
		}
	}

	@Override
	public void setParam(String k, String string) {
		if (k.equalsIgnoreCase("default")){
			try{
				defaultValue = Integer.parseInt(string);
			}
			catch (NumberFormatException e) {
			}
		}

	}

}
