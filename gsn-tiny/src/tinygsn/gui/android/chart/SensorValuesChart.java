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

public class SensorValuesChart extends AbstractDemoChart {
	ArrayList<Double> data = null;
	String vsName, title = "";

	private static final long HOUR = 3600 * 1000;

	private static final long DAY = HOUR * 24;

	// private static final int HOURS = 24;

	public SensorValuesChart(String vsName, String fieldName, ArrayList<Double> result) {
		data = result;
		title = fieldName;
		this.vsName = vsName;
	}

	/**
	 * Returns the chart name.
	 * 
	 * @return the chart name
	 */
	public String getName() {
		return "Sensor chart";
	}

	/**
	 * Returns the chart description.
	 * 
	 * @return the chart description
	 */
	public String getDesc() {
		return "View stream elements as charts";
	}

	public Intent execute(Context context) {

		String[] titles = new String[] { title };
		long now = Math.round(new Date().getTime() / DAY) * DAY;
		List<Date[]> x = new ArrayList<Date[]>();

		int numOfValue = data.size();

		for (int i = 0; i < titles.length; i++) {
			Date[] dates = new Date[numOfValue];
			for (int j = 0; j < numOfValue; j++) {

				dates[j] = new Date(now - (numOfValue - j) * HOUR);
			}
			x.add(dates);
		}

		List<double[]> values = new ArrayList<double[]>();

		double[] d = new double[data.size()];
		List<Double> yValues = new ArrayList<Double>();

		for (int i = 0; i < data.size(); i++) {
			d[i] = data.get(i);
//			Log.v("value " + i + "=", d[i] + "");
			yValues.add(d[i]);
		}

		double minY = Collections.min(yValues);
		double maxY = Collections.max(yValues);

		values.add(d);

		int[] colors = new int[] { Color.GREEN };
		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		setChartSettings(renderer, vsName + " chart", "Time", title,
				x.get(0)[0].getTime(), x.get(0)[numOfValue - 1].getTime(), minY * 2
						- maxY, maxY * 2 - minY, Color.LTGRAY, Color.LTGRAY);

		renderer.setXLabels(10);
		renderer.setYLabels(10);
		renderer.setShowGrid(true);
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		Intent intent = ChartFactory.getTimeChartIntent(context,
				buildDateDataset(titles, x, values), renderer, "h:mm a");

		return intent;
	}

}
