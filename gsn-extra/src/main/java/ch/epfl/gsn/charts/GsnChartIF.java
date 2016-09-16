/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/ch/epfl/gsn/charts/GsnChartIF.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package ch.epfl.gsn.charts;

import java.util.Collection;

import org.jfree.chart.JFreeChart;

import ch.epfl.gsn.reports.beans.Data;

public interface GsnChartIF {
    
    public JFreeChart createChart (Collection<Data> datas) ;

}
