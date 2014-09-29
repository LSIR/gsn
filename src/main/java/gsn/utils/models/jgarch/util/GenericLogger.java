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
* File: src/gsn/utils/models/jgarch/util/GenericLogger.java
*
* @author Saket Sathe
* @author Sofiane Sarni
*
*/

package gsn.utils.models.jgarch.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class GenericLogger {
	
	private static GenericLogger ref;
	
	private static boolean isEnabled = true;
	
	private static Logger logger = Logger.getLogger("anormLogger");

	private GenericLogger() {
		try {
			FileHandler handler = new FileHandler("/tmp/proj.log");
			handler.setFormatter(new Formatter() {
				public String format(LogRecord rec) {
		            StringBuffer buf = new StringBuffer(1000);
		            buf.append(new java.util.Date());
		            buf.append(' ');
		            buf.append(rec.getLevel());
		            buf.append(' ');
		            buf.append(rec.getSourceClassName() + "." + rec.getSourceMethodName());
		            buf.append(' ');
		            buf.append(formatMessage(rec));		            
		            buf.append('\n');
		            return buf.toString();
		            }				
			});
			GenericLogger.logger.addHandler(handler);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}


	}    

	/**
	 * This static method returns the reference of the logger.
	 * @return the reference of the logger.
	 */
	public static synchronized GenericLogger getLogger(){
		if (ref == null) {
			ref = new GenericLogger();
		}
		return ref;
	}   
	
	public void info(String msg){
		if (isEnabled) {
			logger.info(msg);
		}

	}
	
	public void enable() {
		isEnabled = true;
	}

	public void disable() {
		isEnabled = false;
	}	
}
