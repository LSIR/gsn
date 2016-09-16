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
* File: src/ch/epfl/gsn/utils/Helpers.java
*
* @author Timotee Maret
* @author Ali Salehi
* @author Sofiane Sarni
*
*/

package ch.epfl.gsn.utils;

import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class Helpers {

	public static String formatTimePeriod (long timestamp) {
		if (timestamp < 1000) return timestamp + " ms";
		if (timestamp < 60 * 1000) return (timestamp / 1000) + " sec";
		if (timestamp < 60 * 60 * 1000) return (timestamp / (1000 * 60)) + " min";
		if (timestamp < 24 * 60 * 60 * 1000) return (timestamp / (1000 * 60 * 60)) + " h";
		return (timestamp / (24 * 1000 * 60 * 60)) + " day";
	}
	public static long convertTimeFromIsoToLong(String time) throws Exception {
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		return fmt.parseDateTime(time).getMillis();
	}

    public static long convertTimeFromIsoToLong(String time, String format) throws Exception {
		DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
		return fmt.parseDateTime(time).getMillis();
	}

    public static String convertTimeFromLongToIso(long timestamp) {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        DateTime dt = new DateTime(timestamp);
        return fmt.print(dt);
	}

    public static String convertTimeFromLongToIso(long timestamp, String format) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
        DateTime dt = new DateTime(timestamp);
        return fmt.print(dt);
	}

}


