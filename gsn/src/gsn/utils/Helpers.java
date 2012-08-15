package gsn.utils;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class Helpers {
	public static void initLoggerToDebug(){
		Properties properties = new Properties( );
		properties.put( "log4j.rootLogger" , "DEBUG,console" );
		properties.put( "log4j.appender.console" , "org.apache.log4j.ConsoleAppender" );
		properties.put( "log4j.appender.console.Threshold" , "DEBUG" );
		properties.put( "log4j.logger.com.mchange" , "WARN" );
		properties.put( "log4j.logger.org.mortbay" , "WARN" );
		properties.put( "log4j.logger.org.apache" , "WARN" );
		properties.put( "log4j.appender.console.layout" , "org.apache.log4j.PatternLayout" );
		properties.put( "log4j.appender.console.layout.ConversionPattern" , "%-6p[%d] [%t] (%13F:%L) %3x - %m%n" );
		PropertyConfigurator.configure( properties );
	}

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

    
	
/*
	Modified version (by Tonio Gsell) of:
	
	gpstimeutil.js: a javascript library which translates between GPS and unix time
	
	Copyright (C) 2012  Jeffery Kline
	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
	
	*/
	
	// 
	// v0.0: Sat May 19 22:24:16 CDT 2012
	//     initial release
	// v0.1: Sat May 19 22:24:31 CDT 2012
	//     fix bug converting negative fractional gps times
	//     fix bug converting negative fractional unix times
	//     introduce global variable
	// v0.2: Sat May 19 23:08:05 CDT 2012
	//     ensure that unix2gps/gps2unix treats all input/output as Number
	
	/*
	Javascript code is based on original at:
	http://www.andrews.edu/~tzs/timeconv/timealgorithm.html
	
	The difference between the original and this version is that this
	version handles the leap seconds using linear interpolation, not a
	discontinuity.  Linear interpolation guarantees a 1-1 correspondence
	between gps times and unix times.
	
	By contrast, for example, the original implementation maps both gps
	times 46828800.5 and 46828800 map to unix time 362793599.5 
	*/
	private static final long GPS_OFFSET = 315964800L;

	private static final long[] leaps = {46828800L, 78364801L, 109900802L, 173059203L, 252028804L,
		315187205L, 346723206L, 393984007L, 425520008L, 457056009L, 504489610L,
		551750411L, 599184012L, 820108813L, 914803214L, 1025136015L,
		1341118800L};
	
	public static double convertGPSTimeToUnixTime(double gpsSec, short gpsWeek) {
		double gpsTime = (double)(gpsWeek*604800 + gpsSec);
		
		if ( gpsTime < 0)
			return gpsTime + GPS_OFFSET;
		
		double fpart = gpsTime % 1;
		long ipart = (long) Math.floor(gpsTime);

		long leap = countleaps(ipart, false);
		double unixTime = (double)(ipart + GPS_OFFSET - leap);
		
		if (isleap(ipart + 1))
			unixTime = unixTime + fpart / 2;
		else if (isleap(ipart))
			unixTime = unixTime + (fpart + 1) / 2;
		else
			unixTime = unixTime + fpart;
		
		return unixTime;
	}
	
	private static boolean isleap(long gpsTime) {
		boolean isLeap = false;
		for (int i = 0; i < leaps.length; i++) {
			if (gpsTime == leaps[i]) {
				isLeap = true;
				break;
			}
		}
		return isLeap;
	}
	
	private static long countleaps(long gpsTime, boolean accum_leaps) {
		long nleaps = 0;
		
		if (accum_leaps) {
			for (int i = 0; i < leaps.length; i++)
				if (gpsTime + i >= leaps[i])
					nleaps++;
		}
		else {
			for (int i = 0; i < leaps.length; i++)
				if (gpsTime >= leaps[i])
					nleaps++;
		}
		
		return nleaps;
	}

}


