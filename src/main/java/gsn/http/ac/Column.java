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
* File: src/gsn/http/ac/Column.java
*
* @author Behnaz Bostanipour
*
*/

package gsn.http.ac;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 16, 2010
 * Time: 6:35:18 PM
 * To change this template use File | Settings | File Templates.
 */

/*
This class defines a column of a DB table:
 */
    
public class Column
{
    protected String columnLabel;//name of the column
    protected String columnValue; // value of the column

    public Column(String columnLabel)
    {
        this.columnLabel=columnLabel;
    }
    public Column(String columnLabel, String columnValue)
    {
        this.columnLabel=columnLabel;
        this.columnValue=columnValue;
    }
}
