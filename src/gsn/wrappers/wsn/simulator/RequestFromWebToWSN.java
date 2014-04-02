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
* File: src/gsn/wrappers/wsn/simulator/RequestFromWebToWSN.java
*
* @author Ali Salehi
*
*/

package gsn.wrappers.wsn.simulator;

public class RequestFromWebToWSN {
   
   private int             nodeId;
   
   private int             action;
   
   public static final int ASK_FOR_TEMPREATURE = 1;
   
   public static final int ASK_FOR_HIGHER_RATE = 2;
   
   public static final int ASK_FOR_LOWER_RATE  = 3;
   
   public static final int ASK_TO_STOP         = 4;
   
   public static final int ASK_TO_START        = 5;
   
   public RequestFromWebToWSN ( int nodeId , int action ) {
      this.nodeId = nodeId;
      this.action = action;
   }
   
   public int getNodeId ( ) {
      return nodeId;
   }
   
   public int getAction ( ) {
      return action;
   }
   
   public String toString ( ) {
      return "RequestFromWebToWSN{" + "nodeId=" + nodeId + ", action=" + action + '}';
   }
   
   public boolean equals ( Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      
      final RequestFromWebToWSN that = ( RequestFromWebToWSN ) o;
      
      if ( action != that.action ) return false;
      if ( nodeId != that.nodeId ) return false;
      
      return true;
   }
   
   public int hashCode ( ) {
      int result;
      result = nodeId;
      result = 29 * result + action;
      return result;
   }
}
