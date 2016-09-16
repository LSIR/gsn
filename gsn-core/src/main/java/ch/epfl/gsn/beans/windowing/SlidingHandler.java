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
* File: src/ch/epfl/gsn/beans/windowing/SlidingHandler.java
*
* @author gsn_devs
* @author Mehdi Riahi
*
*/

package ch.epfl.gsn.beans.windowing;

import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.beans.StreamSource;

public interface SlidingHandler {
	
	public void addStreamSource(StreamSource streamSource);
	
	public void removeStreamSource(StreamSource streamSource);

	public boolean dataAvailable(StreamElement streamElement);

	public boolean isInterestedIn(StreamSource streamSource);

	public String getCuttingCondition();
	
	public void dispose();

}
