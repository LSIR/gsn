package gsn.reports.scriptlets;

import gsn.charts.GsnChartIF;
import gsn.charts.GsnChartJfreechart;
import gsn.reports.beans.Data;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.renderers.JCommonDrawableRenderer;
import org.apache.commons.math.stat.StatUtils;
import org.jfree.chart.JFreeChart;

public class StreamScriptlet  extends JRDefaultScriptlet {

	private static SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss Z");
	
	private static GsnChartIF gsnChart = new GsnChartJfreechart();

	public StreamScriptlet () {
		super () ;
	}

	public void afterDetailEval() throws JRScriptletException {
		setStatistics () ;
		setGraphic () ;
	}

	@SuppressWarnings("unchecked")
	public void setGraphic () throws JRScriptletException {
		JFreeChart chart = gsnChart.createChart((Collection<Data>) this.getFieldValue("datas"));
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
