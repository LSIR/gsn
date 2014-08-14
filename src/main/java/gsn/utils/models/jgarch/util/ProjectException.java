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
* File: src/gsn/utils/models/jgarch/util/ProjectException.java
*
* @author Saket Sathe
* @author Sofiane Sarni
*
*/

package gsn.utils.models.jgarch.util;

public class ProjectException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8882449588062940944L;

	public ProjectException(String reason){
        super(reason);
    }


}
