/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/gui/android/chart/SensorValuesChart.java
*
* @author Do Ngoc Hoan
*/

package tinygsn.gui.android.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;

public class SensorValuesChart extends AbstractChart {
	ArrayList<Double> y = null;
	ArrayList<Long> x = null;
	String vsName, title = "";


	public SensorValuesChart(String vsname, String fieldName, ArrayList<Long> times, ArrayList<Double> values) {
		y = values;
		x = times;
		title = fieldName;
		vsName = vsname;
	}


	public Intent execute(Context context) {

		String[] titles = new String[] { title };
		List<Date[]> xd = new ArrayList<Date[]>();
		List<double[]> yd = new ArrayList<double[]>();

		int numOfValue = y.size();

		Date[] dates = new Date[numOfValue];
		double[] values = new double[numOfValue];
		
		for (int j = 0; j < numOfValue; j++) {
			dates[j] = new Date(x.get(j));
			values[j] = y.get(j);
		}
		xd.add(dates);
		yd.add(values);

		double minY = Collections.min(y);
		double maxY = Collections.max(y);

		int[] colors = new int[] { Color.GREEN };
		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		setChartSettings(renderer, title + " from " + vsName, "Time", title,
				x.get(0), x.get(numOfValue - 1), minY * 2
						- maxY, maxY * 2 - minY, Color.LTGRAY, Color.LTGRAY);

		renderer.setXLabels(10);
		renderer.setYLabels(10);
		renderer.setShowGrid(true);
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		Intent intent = ChartFactory.getTimeChartIntent(context,
				buildDateDataset(titles, xd, yd), renderer, "h:mm a");

		return intent;
	}

}
