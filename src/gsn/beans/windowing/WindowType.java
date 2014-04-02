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
* File: src/gsn/beans/windowing/WindowType.java
*
* @author gsn_devs
*
*/

package gsn.beans.windowing;

public enum WindowType {
	TIME_BASED, TUPLE_BASED, TIME_BASED_WIN_TUPLE_BASED_SLIDE, TUPLE_BASED_WIN_TIME_BASED_SLIDE, TUPLE_BASED_SLIDE_ON_EACH_TUPLE, TIME_BASED_SLIDE_ON_EACH_TUPLE;

	public static boolean isTimeBased(WindowType windowType) {
		return windowType == TIME_BASED || windowType == TIME_BASED_SLIDE_ON_EACH_TUPLE || windowType == TUPLE_BASED_WIN_TIME_BASED_SLIDE;
	}

	public static boolean isTupleBased(WindowType windowType) {
		return !isTimeBased(windowType);
	}
}
