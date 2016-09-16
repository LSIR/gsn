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
* File: src/ch/epfl/gsn/utils/Utils.java
*
* @author Timotee Maret
*
*/

package ch.epfl.gsn.utils;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.utils.Pair;
import ch.epfl.gsn.utils.Utils;

import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Utils {

    private static final transient Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Properties loadProperties(String path) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(path));
        } catch (IOException e) {
            logger.warn("Unable to load the property file: " + path);
            return null;
        }
        return properties;
    }
    
    public static Pair<Boolean,Long> parseWindowSize(String s) throws NumberFormatException{
		s = s.replace( " " , "" ).trim( ).toLowerCase( );
		int mIndex = s.indexOf( "m" );
		int hIndex = s.indexOf( "h" );
		int sIndex = s.indexOf( "s" );
		if ( mIndex < 0 && hIndex < 0 && sIndex < 0 ) {
			return new Pair<Boolean,Long>(false,Long.parseLong(s));
		} else {
			StringBuilder shs = new StringBuilder(s);
			long value = 0;
			if ( mIndex >= 0 && mIndex == shs.length() - 1) value = Long.parseLong( shs.deleteCharAt( mIndex ).toString( ) ) * 60000;
			else if ( hIndex >= 0 && hIndex == shs.length() - 1) value = Long.parseLong( shs.deleteCharAt( hIndex ).toString( ) ) * 3600000;
			else if ( sIndex >= 0 && sIndex == shs.length() - 1) value = Long.parseLong( shs.deleteCharAt( sIndex ).toString( ) ) * 1000;
			else throw new NumberFormatException("unable to pasre window size :"+shs);
			return new Pair<Boolean,Long>(true,value);
		} 
    }

}
