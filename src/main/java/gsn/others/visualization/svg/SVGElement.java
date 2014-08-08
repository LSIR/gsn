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
* File: src/gsn/others/visualization/svg/SVGElement.java
*
* @author Ali Salehi
*
*/

package gsn.others.visualization.svg;

public interface SVGElement {
   
   /**
    * Draws the SVG tags of this SVGElement and its children on the specified
    * input.
    * 
    * @param input The SVG tags will be written on the input.
    */
   public void drawOn ( StringBuilder input );
   
   /**
    * Gets the name of this SVGElement. The name is typically the toString of
    * the internal Object.
    * 
    * @return Null if the internal object is null or toString() method of the
    * internal object returns null.
    */
   public String getName ( );
   
   /**
    * Gets the Opacity value.
    * 
    * @return
    */
   public double getOpaqeAlpha ( );
   
   /**
    * Sets the opactiy of the SVGElement. Typically the valus is 1. This can be
    * used to generate nice looking graphic effects.
    * 
    * @param value
    */
   
   public void setOpacity ( double value );
   
   /**
    * Sets the URL link associated with the SVG Element.
    * 
    * @param link
    */
   public void setLink ( String link );
   
   /**
    * Sets the internal object inside the SVGElement
    * 
    * @param o
    */
   public void setObject ( Object o );
   
   /**
    * Gets the internal object inside the SVGElement
    * 
    * @return
    */
   public Object getObject ( );
   
   /**
    * If a SVGElement is disable, it will not be used. Set the value for enable
    * field.
    * 
    * @param enable
    */
   public void setEnable ( boolean enable );
   
   public boolean isEnable ( );
}
