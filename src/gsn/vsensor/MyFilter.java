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
* File: src/gsn/vsensor/MyFilter.java
*
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package gsn.vsensor;

import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class MyFilter extends AbstractVirtualSensor {

  private static final transient Logger logger = Logger.getLogger( BridgeVirtualSensor.class );

  public boolean initialize ( ) {
    return true;
  }

  public void dataAvailable ( String inputStreamName , StreamElement data ) {
    
    dataProduced( data );
    if ( logger.isDebugEnabled( ) ) logger.debug( "Data received under the name: " + inputStreamName );
  }

  public void dispose ( ) {

  }

}
