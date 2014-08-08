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
* File: src/gsn/wrappers/ieee1451/TedsToVSResult.java
*
* @author Ali Salehi
*
*/

package gsn.wrappers.ieee1451;

public class TedsToVSResult {
   
   public static String ERROR   = "Error";
   
   public static String ADDED   = "Added";
   
   public static String REMOVED = "Removed";
   
   public static String NOTHING = "Nothing";
   
   public String        fileName;
   
   public String        status;
   
   public String        tedsHtmlString;
   
   /**
    * Possible values for TedsID are : <br>
    * MicaTWO, MicaONE,MicaTHREE
    */
   public String        tedsID;
   
   public TedsToVSResult ( String fileName , int status , String tedsHtmlString , String tedsID ) {
      this.fileName = fileName;
      this.status = statusString( status );
      this.tedsHtmlString = tedsHtmlString;
      this.tedsID = tedsID;
   }
   
   public TedsToVSResult ( int status ) {
      this.status = statusString( status );
   }
   
   private String statusString ( int status ) {
      String result;
      switch ( status ) {
         case -1 :
            result = ERROR;
            break;
         case 0 :
            result = ADDED;
            break;
         case 1 :
            result = REMOVED;
            break;
         default :
            result = NOTHING;
            
      }
      return result;
      
   }
}
