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
* File: src/gsn/charts/GsnChartJfreechart.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package gsn.charts;

import gsn.reports.beans.Data;

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

public class GsnChartJfreechart implements GsnChartIF {
	
	private static SimpleDateFormat ssdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	
	public static transient Logger logger= Logger.getLogger ( GsnChartJfreechart.class );

	private static final Font TICK_FONT = new Font("Helvetica", Font.PLAIN, 7) ;
	
	public GsnChartJfreechart () {}
	
	public JFreeChart createChart(Collection<Data> datas) {
		TimeSeries t1 = new TimeSeries("S1");
		Iterator<Data> iter = datas.iterator() ; 
		Data data ;
		while (iter.hasNext()) {
			data = iter.next();
			t1.addOrUpdate(RegularTimePeriod.createInstance(Millisecond.class, new Date((Long)data.getP2()), TimeZone.getDefault()), data.getValue());
		}
		XYDataset dataset = new TimeSeriesCollection(t1);
		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				null,
				null, 
				null, 
				dataset, 
				false,
				false, 
				false
		);
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);
		chart.setBackgroundPaint(Color.WHITE);
		//
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setNoDataMessage("No Data to Display");
		plot.setDomainGridlinesVisible(true);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setInsets(new RectangleInsets(5,14,0,5));
		//
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(ssdf);
		axis.setTickLabelFont(TICK_FONT);
		ValueAxis rangeAxis = plot.getRangeAxis();
		rangeAxis.setTickLabelFont(TICK_FONT);
		//
		return chart;
	}
}
