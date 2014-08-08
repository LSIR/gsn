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
* File: src/gsn/wrappers/ieee1451/Record.java
*
* @author Ali Salehi
*
*/

package gsn.wrappers.ieee1451;

/**
 * An aggregation or collection of related measurements. Measurements are stored
 * along with a name. This name is used to retrieve the Measurement instance.
 * <p>
 */
public class Record extends ArgArray {
   
   /**
    * Clone the given Record instance.
    * 
    * @param value
    */
   public Record ( Record value ) {
      super( );
      value.cloneContentsTo( this );
   }
   
   /**
    * Clone the given ArgArray instance
    * 
    * @param value
    */
   public Record ( ArgArray value ) {
      super( );
      value.cloneContentsTo( this );
   }
   
   /**
    * Creates a blank record with no comments.
    */
   public Record ( ) {

   }
}
