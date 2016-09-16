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
* File: src/ch/epfl/gsn/wrappers/tinyos/TinyOS2XTemplate.java
*
* @author Ali Salehi
*
*/

package ch.epfl.gsn.wrappers.tinyos;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixSource;
/**
 * A simple test class to see if the tinyos connection is working fine by simply asking for some data from the serial forwarder. 

 *
 */
public class TinyOS2XTemplate {
   /**
    * A Test class to check if the wrapper works.
    * @param args
    * @throws Exception
    */
   public static void main ( String [ ] args ) throws Exception {
      PhoenixSource reader = BuildSource.makePhoenix( BuildSource.makeSF( "eflumpc24.epfl.ch" , 2020 ), null );
      reader.start( );
      MoteIF moteif = new MoteIF( reader );
      moteif.registerListener( new SensorScopeDataMsg( ) , new MessageListener( ) {
         public void messageReceived ( int dest , Message rawMsg ) {
            System.out.println( "Received." );
         }
      } );
   }
}
