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
* File: src/gsn/utils/protocols/EPuck/SerComProtocol.java
*
* @author Jerome Rousselot
* @author Ali Salehi
*
*/

package gsn.utils.protocols.EPuck;

import gsn.utils.protocols.AbstractHCIProtocol;
import gsn.utils.protocols.BasicHCIQuery;

public class SerComProtocol extends AbstractHCIProtocol {
   
   public static final String EPUCK_PROTOCOL="EPUCK_PROTOCOL";
   // wait time in ms for an answer to a query
   public static final int EPUCK_DEFAULT_WAIT_TIME = 250;
      
   // add here a short name for the created query. This name
   // will be shown to the user so that he can choose between queries.
   public static final String SET_SPEED="Set speed", RESET="Reset";
      
   public SerComProtocol() {
	   super(EPUCK_PROTOCOL);
      // Create and add here a query of each type
      //0. Add always useful user-customisable query
	   addQuery(new BasicHCIQuery());
	   // 1. Add RESET command
      addQuery(new Reset(RESET));
      //2. Add SET_SPEED command
      addQuery(new SetSpeed(SET_SPEED));
      
   }

}
