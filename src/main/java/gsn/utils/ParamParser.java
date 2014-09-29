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
* File: src/gsn/utils/ParamParser.java
*
* @author Ali Salehi
*
*/

package gsn.utils;

public class ParamParser {
   
   public static int getInteger ( String input , int defaultValue ) {
      if ( input == null ) return defaultValue;
      try {
         return Integer.parseInt( input );
      } catch ( Exception e ) {
         return defaultValue;
      }
   }
   
   public static int getInteger ( Object input , int defaultValue ) {
      if ( input == null ) return defaultValue;
      try {
         if ( input instanceof String ) return getInteger( ( String ) input , defaultValue );
         if ( input instanceof Number ) return ( ( Number ) input ).intValue( );
         else
            return Integer.parseInt( input.toString( ) );
      } catch ( Exception e ) {
         return defaultValue;
      }
   }
}
