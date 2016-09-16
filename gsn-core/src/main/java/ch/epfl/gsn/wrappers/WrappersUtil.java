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
* File: src/ch/epfl/gsn/wrappers/WrappersUtil.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package ch.epfl.gsn.wrappers;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.wrappers.WrappersUtil;

import org.slf4j.Logger;
public class WrappersUtil {
  
  public static transient Logger logger= LoggerFactory.getLogger ( WrappersUtil.class );
  
  public static final String     DEFAULT_WRAPPER_PROPERTIES_FILE  = Main.gsnConfFolder +"/wrappers.properties";
  public static Properties loadWrappers(HashMap<String, Class<?>> wrappers, String location) {
    Properties config = new Properties ();
    try {// Trying to load the wrapper specified in the configuration file of the container. 
      config.load(new FileReader( location ));
    } catch ( IOException e ) {
      logger.error ( new StringBuilder ( ).append ( "Check the :" ).append ( location ).append ( " file and make sure it's syntactically correct: " ).append(e.getMessage()).toString ( ) );
      logger.info ( "Sample wrappers extention properties file is provided in GSN distribution." );
      System.exit ( 1 );
    }  
   // TODO: Checking for duplicates in the wrappers file.
    return config;
  }  
  public static Properties loadWrappers(HashMap<String, Class<?>> wrappers){
    return loadWrappers(wrappers,DEFAULT_WRAPPER_PROPERTIES_FILE);
  }
}
