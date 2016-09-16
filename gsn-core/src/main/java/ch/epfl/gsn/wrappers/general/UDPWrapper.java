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
* File: src/ch/epfl/gsn/wrappers/general/UDPWrapper.java
*
* @author Ali Salehi
* @author Mehdi Riahi
* @author Sofiane Sarni
*
*/

package ch.epfl.gsn.wrappers.general;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.wrappers.AbstractWrapper;

import org.slf4j.Logger;

/**
 * Links GSN to a Wisenet sensors network. The computer running this wrapper
 * should be connected to an IP network. One of the WSN nodes should forward
 * received packets through UDP to the host running this wrapper.
 */
public class UDPWrapper extends AbstractWrapper {
   
   private static final String    RAW_PACKET    = "RAW_PACKET";
   
   private final transient Logger logger        = LoggerFactory.getLogger( UDPWrapper.class );
   
   private int                    threadCounter = 0;
   
   public InputStream             is;
   
   private AddressBean            addressBean;
   
   private int                    port;
   
   private DatagramSocket         socket;
   
   /*
    * Needs the following information from XML file : port : the udp port it
    * should be listening to rate : time to sleep between each packet
    */
   public boolean initialize (  ) {
      addressBean = getActiveAddressBean( );
      try {
         port = Integer.parseInt( addressBean.getPredicateValue( "port" ) );
         socket = new DatagramSocket( port );
      } catch ( Exception e ) {
         logger.warn( e.getMessage( ) , e );
         return false;
      }
      return true;
   }
   
   public void run ( ) {
      byte [ ] receivedData = new byte [ 50 ];
      DatagramPacket receivedPacket = null;
      while ( isActive( ) ) {
         try {
            receivedPacket = new DatagramPacket( receivedData , receivedData.length );
            socket.receive( receivedPacket );
            String dataRead = new String( receivedPacket.getData( ) );
            logger.debug( "UDPWrapper received a packet : " + dataRead );
            StreamElement streamElement = new StreamElement( new String [ ] { RAW_PACKET } , new Byte [ ] { DataTypes.BINARY } , new Serializable [ ] { receivedPacket.getData( ) } , System
                  .currentTimeMillis( ) );
            postStreamElement( streamElement );
         } catch ( IOException e ) {
            logger.warn( "Error while receiving data on UDP socket : " + e.getMessage( ) );
         }
      }
   }
   
   public  DataField [] getOutputFormat ( ) {
      return new DataField[] {new DataField( RAW_PACKET , "BINARY" , "The packet contains raw data received as a UDP packet." ) };
     
   }
   
   public void dispose (  ) {
      socket.close();
      threadCounter--;
   }
   public String getWrapperName() {
    return "network udp";
}
   public static void main ( String [ ] args ) {
   // To check if the wrapper works properly.
   // this method is not going to be used by the system.
   }
}
