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
* File: src/gsn/wrappers/wsn/simulator/DataPacket.java
*
* @author Ali Salehi
*
*/

package gsn.wrappers.wsn.simulator;

public class DataPacket {
   
   private int             parent;
   
   private int             identifier;
   
   private int             value;
   
   private int             typeOfPacket;
   
   public static final int ROUTING_AND_DATA_PACKET    = 1;
   
   public static final int ROUTING_WITHOUT_DATA       = 2;
   
   public static final int TEMPREATURE_REQUEST_PACKET = 3;
   
   public int getTypeOfPacket ( ) {
      return typeOfPacket;
   }
   
   public DataPacket ( int identifier , int parent , int tempreature , int typeOfThePacket ) {
      this.identifier = identifier;
      this.parent = parent;
      this.value = tempreature;
      this.typeOfPacket = typeOfThePacket;
   }
   
   public int getParent ( ) {
      return parent;
   }
   
   public int getIdentifier ( ) {
      return identifier;
   }
   
   public int getValue ( ) {
      return value;
   }
   
   public String toString ( ) {
      return "DataPacket{" + "parent=" + parent + ", identifier=" + identifier + ", value=" + value + '}';
   }
}
