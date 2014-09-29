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
* File: src/gsn/others/visualization/svg/Width_Height_Decendent_ValueBean.java
*
* @author Ali Salehi
*
*/

package gsn.others.visualization.svg;

public class Width_Height_Decendent_ValueBean {
   
   String [ ] stringTokenizer;
   
   int        width;
   
   int [ ]    heights;
   
   int [ ]    decendents;
   
   public Width_Height_Decendent_ValueBean ( String [ ] stringTokenizer , int width , int heights[] , int decendents[] ) {
      this.stringTokenizer = stringTokenizer;
      this.width = width;
      this.heights = heights;
      this.decendents = decendents;
   }
   
   public String [ ] getStringTokenizer ( ) {
      return stringTokenizer;
   }
   
   public void setStringTokenizer ( String [ ] stringTokenizer ) {
      this.stringTokenizer = stringTokenizer;
   }
   
   public int getWidth ( ) {
      return width;
   }
   
   public void setWidth ( int width ) {
      this.width = width;
   }
   
   public int [ ] getHeights ( ) {
      return heights;
   }
   
   public void setHeights ( int [ ] heights ) {
      this.heights = heights;
   }
   
   public int [ ] getDecendents ( ) {
      return decendents;
   }
   
   public void setDecendents ( int [ ] decendents ) {
      this.decendents = decendents;
   }
   
   /**
    * Starts from zero to length of the total available heights The
    * getTotalHeightUpTo(0) is zero.
    */
   public int getTotalHeightUpTo ( int lineCounter ) {
      if ( lineCounter < 0 || lineCounter > stringTokenizer.length ) throw new RuntimeException( "Outof the bound exception : " + lineCounter );
      
      int toReturn = 0;
      for ( int i = 0 ; i < lineCounter ; i++ )
         toReturn += heights[ i ];
      return toReturn;
   }
}
