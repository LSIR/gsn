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
* File: src/gsn/utils/CaseInsensitiveComparator.java
*
* @author Mehdi Riahi
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn.utils;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Note that this class will trim all the space characters surrounding the key
 * and value pairs hence you don't need to call trim when putting or getting a
 * value to/from the hashmap.
 */
public class CaseInsensitiveComparator implements Comparator,Serializable {
   
private static final long serialVersionUID = 2540687777213332025L;

public int compare ( Object o1 , Object o2 ) {
      if ( o1 == null && o2 == null ) return 0;
      if ( o1 == null ) return -1;
      if ( o2 == null ) return 1;
      String input1 = o1.toString( ).trim( );
      String input2 = o2.toString( ).trim( );
      return input1.compareToIgnoreCase( input2 );
   }
}
