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
* File: src/gsn/utils/graph/EdgeExistsException.java
*
* @author Mehdi Riahi
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn.utils.graph;

public class EdgeExistsException extends Exception {

	private static final long serialVersionUID = 6890337360223725923L;

	public EdgeExistsException() {
		super();
	}

	public EdgeExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public EdgeExistsException(String message) {
		super(message);
	}

	public EdgeExistsException(Throwable cause) {
		super(cause);
	}
	
}
