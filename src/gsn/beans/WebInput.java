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
* File: src/gsn/beans/WebInput.java
*
* @author Mehdi Riahi
* @author gsn_devs
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn.beans;

import java.io.Serializable;

public class WebInput implements Serializable{
   
	private static final long serialVersionUID = 1587176728962536853L;

	private String name;
  
  private DataField[] parameters;
   
   /**
    * @return the commandName
    */
   public String getName ( ) {
      return name;
   }
   
   public void setName(String name){
	   this.name = name;
   }
   
   /**
    * @return the inputParams
    */
   public DataField [ ] getParameters ( ) {
      return parameters;
   }
   
   public void setParameters(DataField[ ] parameters){
	   this.parameters = parameters;
   } 
}
