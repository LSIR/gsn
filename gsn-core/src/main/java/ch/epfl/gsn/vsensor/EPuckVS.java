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
* File: src/ch/epfl/gsn/vsensor/EPuckVS.java
*
* @author Jerome Rousselot
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package ch.epfl.gsn.vsensor;

import java.util.TreeMap;

import javax.naming.OperationNotSupportedException;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.utils.protocols.ProtocolManager;
import ch.epfl.gsn.utils.protocols.EPuck.SerComProtocol;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;
import ch.epfl.gsn.vsensor.EPuckVS;
import ch.epfl.gsn.wrappers.AbstractWrapper;
import ch.epfl.gsn.wrappers.general.SerialWrapper;

import org.slf4j.Logger;

public class EPuckVS extends AbstractVirtualSensor {
   
   private static final transient Logger logger = LoggerFactory.getLogger( EPuckVS.class );
   
   private TreeMap < String , String >   params;
   
   private ProtocolManager               protocolManager;
   
   private AbstractWrapper                       wrapper;
   
   private VSensorConfig                 vsensor;
   
   public boolean initialize ( ) {
      params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
      wrapper = getVirtualSensorConfiguration( ).getInputStream( "input1" ).getSource( "source1" ).getWrapper( );
      protocolManager = new ProtocolManager( new SerComProtocol( ) , wrapper );
      logger.debug( "Created protocolManager" );
      try {
         wrapper.sendToWrapper( "h\n",null,null );
      } catch ( OperationNotSupportedException e ) {
    	  logger.error(e.getMessage(), e);
      }
      // protocolManager.sendQuery( SerComProtocol.RESET , null );
      logger.debug( "Initialization complete." );
      return true;
   }
   
   boolean actionA = false;
   
   public void dataAvailable ( String inputStreamName , StreamElement data ) {
      logger.debug( "I just received some data from the robot" );
      logger.trace( new String( ( byte [ ] ) data.getData( SerialWrapper.RAW_PACKET ) ) );
      AbstractWrapper wrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getWrapper( );
      if ( actionA == false ) {
         actionA = true;
         try {
            // wrapper.sendToWrapper( "h\n" );
            wrapper.sendToWrapper( "d,1000,-1000\n",null,null );
         } catch ( OperationNotSupportedException e ) {
            logger.error( e.getMessage( ) , e );
         }
      }
   }
   
   public void dispose ( ) {
      try {
         vsensor.getInputStream( "input1" ).getSource( "source1" ).getWrapper().sendToWrapper( "R\n",null,null );
      } catch ( OperationNotSupportedException e ) {
         logger.error( e.getMessage( ) , e );
      }
   }
   
}
