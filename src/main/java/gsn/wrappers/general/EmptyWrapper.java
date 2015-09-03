/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: src/gsn/wrappers/general/EmptyWrapper.java
*
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package gsn.wrappers.general;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.AbstractWrapper;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class EmptyWrapper extends AbstractWrapper {
   
   private final transient Logger               logger        = LoggerFactory.getLogger( EmptyWrapper.class );
   
   private int                                  threadCounter = 0;
   
   private static   DataField [] dataField  ;
   
   public boolean initialize (  ) {
      AddressBean addressBean = getActiveAddressBean( );
      dataField = new DataField[] { new DataField( "DATA" , "int" , "incremental int" ) };
      return true;
   }
   
   public void run ( ) {
      while ( isActive( ) ) {
    	  // do something
      }
   }
   
   public  DataField[] getOutputFormat ( ) {
      return dataField;
   }
   public String getWrapperName() {
    return "empty template";
}
   public void dispose ( ) {
      threadCounter--;
   }
   
}
