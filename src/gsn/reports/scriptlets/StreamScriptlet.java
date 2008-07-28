package gsn.reports.scriptlets;

import gsn.reports.beans.Data;

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.renderers.JCommonDrawableRenderer;
import org.apache.commons.math.stat.StatUtils;
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

public class StreamScriptlet  extends JRDefaultScriptlet {

	private static SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss Z");
	private static SimpleDateFormat ssdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");

	private static final Font TICK_FONT = new Font("Helvetica", Font.PLAIN, 7) ;

	public StreamScriptlet () {
		super () ;
	}

	public void afterDetailEval() throws JRScriptletException {
		setStatistics () ;
		setGraphic () ;
	}

	@SuppressWarnings("unchecked")
	public void setGraphic () throws JRScriptletException {
		Collection<Data> datas = (Collection<Data>) this.getFieldValue("datas");
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
		plot.setInsets(new RectangleInsets(0,14,0,0));
		//
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(ssdf);
		axis.setTickLabelFont(TICK_FONT);
		ValueAxis rangeAxis = plot.getRangeAxis();
		rangeAxis.setTickLabelFont(TICK_FONT);
		//
		this.setVariableValue("Chart", new JCommonDrawableRenderer(chart));
	}

	@SuppressWarnings("unchecked")
	public void setStatistics () throws JRScriptletException {
		Collection<Data> datas = (Collection<Data>) this.getFieldValue("datas");
		double[] dat = new double[datas.size()];
		Iterator<Data> iter = datas.iterator();
		int i = 0;
		boolean first = true;
		String startTime = "NA";
		String endTime = "NA";
		Data next;
		while (iter.hasNext()) {
			next = iter.next();
			if (first) {
				first = false;
				startTime = sdf.format(new Date((Long)next.getP2())).toString();
			}
			if (! iter.hasNext()) endTime = sdf.format(new Date((Long) next.getP2())).toString();
			dat[i] = (Double) next.getValue();
			i++;
		}
		Double max = StatUtils.max(dat);
		Double min = StatUtils.min(dat);
		Double average = StatUtils.mean(dat);
		Double stdDeviation = Math.sqrt(StatUtils.variance(dat));
		Double median;
		if (dat.length > 0) median = dat[dat.length / 2];
		else                median = Double.NaN;
		Integer nb = dat.length;
		//
		this.setVariableValue("max", max.toString());
		this.setVariableValue("min", min.toString());
		this.setVariableValue("average", average.toString());
		this.setVariableValue("stdDeviation", stdDeviation.toString());
		this.setVariableValue("median", median.toString());
		this.setVariableValue("nb", nb.toString());
		this.setVariableValue("startTime", startTime);
		this.setVariableValue("endTime", endTime);
	}
}
