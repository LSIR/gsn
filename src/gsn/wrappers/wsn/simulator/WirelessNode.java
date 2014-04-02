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
* File: src/gsn/wrappers/wsn/simulator/WirelessNode.java
*
* @author Ali Salehi
*
*/

package gsn.wrappers.wsn.simulator;

public class WirelessNode extends Thread implements WirelessNodeInterface {
   
   int                  sleepingTime = 5000;
   
   boolean              alive        = true;
   
   private WirelessNode parent;
   
   private DataListener dataListener = null;
   
   public int getIdentifier ( ) {
      return identity;
   }
   
   private int identity;
   
   public WirelessNode ( int identifier ) {
      this.identity = identifier;
   }
   
   // public WirelessNode ( int identifier, ) {
   // this.identity = identifier;
   // }
   //
   protected void setParent ( WirelessNode parent ) {
      this.parent = parent;
   }
   
   public WirelessNode getParent ( ) {
      return parent;
   }
   
   public void run ( ) {
      while ( alive ) {
         try {
            Thread.sleep( sleepingTime );
         } catch ( InterruptedException e ) {
            e.printStackTrace( ); // To change body of catch statement use
            // File | Settings | File Templates.
         }
         int tempreature = ( int ) ( Math.random( ) * 100 );
         DataPacket dataPacket = new DataPacket( identity , ( parent == null ) ? -1 : parent.getIdentifier( ) , tempreature , DataPacket.ROUTING_AND_DATA_PACKET );
         send( dataPacket );
      }
   }
   
   public void send ( DataPacket e ) {
      if ( parent != null ) {
         parent.send( e );
      } else {
         if ( dataListener != null ) dataListener.newDataAvailable( e );
         else {
            System.out.println( "Data received by the wireless node with the id of = " + identity );
         }
      }
   }
   
   public void stopNode ( ) {
      alive = false;
   }
   
   public void addDataListener ( DataListener wsnsWrapper ) {
      this.dataListener = wsnsWrapper;
   }
}
