/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/vsensor/BridgeVirtualSensor.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.model.vsensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.Environment;

import org.epfl.locationprivacy.util.Utils;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.utils.ParameterType;
import tinygsn.model.utils.Parameter;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import static android.os.Debug.startMethodTracing;
import static android.os.Debug.stopMethodTracing;


/*

y_max- <= -2.444118: 5 (31.0/1.0)
y_max- > -2.444118
|   z_moment-moment-6 <= 1571.756023
|   |   coefficient_variation- <= 0.283575
|   |   |   z_percentile-percentile-25 <= 0.68651
|   |   |   |   z_mean- <= -5.048177
|   |   |   |   |   std- <= 0.293853: 2 (2.0)
|   |   |   |   |   std- > 0.293853: 0 (2.0/1.0)
|   |   |   |   z_mean- > -5.048177
|   |   |   |   |   mode- <= 7.17009
|   |   |   |   |   |   y_mean- <= -2.062379: 1 (9.0/1.0)
|   |   |   |   |   |   y_mean- > -2.062379: 4 (6.0/1.0)
|   |   |   |   |   mode- > 7.17009: 1 (31.0/1.0)
|   |   |   z_percentile-percentile-25 > 0.68651
|   |   |   |   std- <= 0.613081: 1 (2.0/1.0)
|   |   |   |   std- > 0.613081: 3 (32.0)
|   |   coefficient_variation- > 0.283575
|   |   |   z_min- <= -6.830595
|   |   |   |   y_mean- <= -2.287655: 0 (17.0)
|   |   |   |   y_mean- > -2.287655: 4 (4.0)
|   |   |   z_min- > -6.830595: 4 (12.0/1.0)
|   z_moment-moment-6 > 1571.756023: 2 (26.0)

 */


public class ActivityVirtualSensor extends AbstractVirtualSensor {


	private static final long serialVersionUID = 772132288546137586L;
	private Classifier cls_act;
	private String fileName = "activity-julien-norm.model";
	private DataField[] outputStructure = new DataField[]{new DataField("activity", DataTypes.DOUBLE)};

	private String LOGTAG = "ActivityVirtualSensor";


	@Override
	public boolean initialize() {
		try {
			cls_act = (Classifier) SerializationHelper.read(Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/Android/data/tinygsn/" + fileName);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<Parameter> getParameters() {
		ArrayList<Parameter> list = new ArrayList<>();
		list.add(new Parameter("model_file", ParameterType.EDITBOX));
		return list;
	}

	@Override
	protected void initParameter(String key, String value) {
		if (key.endsWith("model_file")) {
			fileName = value;
		}
	}


	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {

	}

	@Override
	public void dataAvailable(String inputStreamName, ArrayList<StreamElement> streamElements) {
		if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE")) {
			startMethodTracing("Android/data/tinygsn.gui.android/" + LOGTAG + "_" + inputStreamName + "_" + System.currentTimeMillis());
		}
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "===========================================");
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Starting to process data in dataAvailable");
		long startLogTime = System.currentTimeMillis();
		if (streamElements.size() < 3) return; //too small to compute

		double[] x = new double[streamElements.size()];
		double[] y = new double[x.length];
		double[] z = new double[x.length];
		double[] norm = new double[x.length];

		double mean_x = 0;
		double mean_y = 0;
		double mean_z = 0;
		double mean_norm = 0;
		double std_x = 0;
		double std_y = 0;
		double std_z = 0;
		double std_norm = 0;
		double min_x = Double.MAX_VALUE;
		double min_y = Double.MAX_VALUE;
		double min_z = Double.MAX_VALUE;
		double min_norm = Double.MAX_VALUE;
		double max_x = Double.MIN_VALUE;
		double max_y = Double.MIN_VALUE;
		double max_z = Double.MIN_VALUE;
		double max_norm = Double.MIN_VALUE;

		for (int i = 0; i < x.length; i++) {
			Serializable[] d = streamElements.get(i).getData();
			x[i] = (Double) d[0];
			y[i] = (Double) d[1];
			z[i] = (Double) d[2];
			norm[i] = Math.sqrt(x[i] * x[i] + y[i] * y[i] + z[i] * z[i]);
			mean_x += x[i];
			mean_y += y[i];
			mean_z += z[i];
			mean_norm += norm[i];
			std_x += x[i] * x[i];
			std_y += y[i] * y[i];
			std_z += z[i] * z[i];
			std_norm += norm[i] * norm[i];
			min_x = x[i] < min_x ? x[i] : min_x;
			min_y = y[i] < min_y ? y[i] : min_y;
			min_z = z[i] < min_z ? z[i] : min_z;
			min_norm = norm[i] < min_norm ? norm[i] : min_norm;
			max_x = x[i] < max_x ? x[i] : max_x;
			max_y = y[i] < max_y ? y[i] : max_y;
			max_z = z[i] < max_z ? z[i] : max_z;
			max_norm = norm[i] < max_norm ? norm[i] : max_norm;
		}
		mean_x /= x.length;
		mean_y /= y.length;
		mean_z /= z.length;
		mean_norm /= norm.length;

		std_x = Math.sqrt(std_x / x.length - mean_x * mean_x);
		std_y = Math.sqrt(std_y / y.length - mean_y * mean_y);
		std_z = Math.sqrt(std_z / z.length - mean_z * mean_z);
		std_norm = Math.sqrt(std_norm / norm.length - mean_norm * mean_norm);

		double range_x = max_x - min_x;
		double range_y = max_y - min_y;
		double range_z = max_z - min_z;
		double range_norm = max_norm - min_norm;

		double cv_x = std_x / mean_x;
		double cv_y = std_y / mean_y;
		double cv_z = std_z / mean_z;
		double cv_norm = std_norm / mean_norm;

		double kurt_x = 0;
		double kurt_y = 0;
		double kurt_z = 0;
		double kurt_norm = 0;

		for (int i = 0; i < x.length; i++) {
			kurt_x += Math.pow(x[i] - mean_x, 4);
			kurt_y += Math.pow(y[i] - mean_y, 4);
			kurt_z += Math.pow(z[i] - mean_z, 4);
			kurt_norm += Math.pow(norm[i] - mean_norm, 4);
		}

		kurt_x = (kurt_x / x.length) / Math.pow(std_x, 4);
		kurt_y = (kurt_y / y.length) / Math.pow(std_y, 4);
		kurt_z = (kurt_z / z.length) / Math.pow(std_z, 4);
		kurt_norm = (kurt_norm / norm.length) / Math.pow(std_norm, 4);

		Arrays.sort(x);
		Arrays.sort(y);
		Arrays.sort(z);
		Arrays.sort(norm);

     /*   double median_x = x.length % 2 == 1 ? x[(x.length - 1)/2] : x[(x.length)/2] + x[(x.length)/2 - 1] / 2.0;
		double median_y = y.length % 2 == 1 ? y[(y.length - 1)/2] : y[(y.length)/2] + y[(y.length)/2 - 1] / 2.0;
        double median_z = z.length % 2 == 1 ? z[(z.length - 1)/2] : z[(z.length)/2] + z[(z.length)/2 - 1] / 2.0;
       */
		double percent25_x = x[(int) Math.round(x.length / 4.0)];
		double percent25_y = y[(int) Math.round(y.length / 4.0)];
		double percent25_z = z[(int) Math.round(z.length / 4.0)];
		double percent25_norm = norm[(int) Math.round(norm.length / 4.0)];

		double percent75_x = x[(int) Math.round(3.0 * x.length / 4)];
		double percent75_y = y[(int) Math.round(3.0 * y.length / 4)];
		double percent75_z = z[(int) Math.round(3.0 * z.length / 4)];
		double percent75_norm = norm[(int) Math.round(3.0 * norm.length / 4)];

        /*double[] vector = new double[]{mean_x,std_x,min_x,max_x,range_x,cv_x,kurt_x,percent25_x,percent75_x,
				                       mean_y,std_y,min_y,max_y,range_y,cv_y,kurt_y,percent25_y,percent75_y,
        		                       mean_z,std_z,min_z,max_z,range_z,cv_z,kurt_z,percent25_z,percent75_z};
		*/


		FastVector v = new FastVector();
		FastVector classVal = new FastVector();
		classVal.addElement("0");
		classVal.addElement("1");
		classVal.addElement("2");
		classVal.addElement("3");
		classVal.addElement("4");
		classVal.addElement("5");
		Attribute label = new Attribute("label", classVal);
		v.addElement(label);
		Attribute mean = new Attribute("julien-norm-mean-");
		v.addElement(mean);
		Attribute std = new Attribute("julien-norm-std-");
		v.addElement(std);
		Attribute min = new Attribute("julien-norm-min-");
		v.addElement(min);
		Attribute max = new Attribute("julien-norm-max-");
		v.addElement(max);
		Attribute range = new Attribute("julien-norm-range-");
		v.addElement(range);
		Attribute cv = new Attribute("julien-norm-coefficient_variation-");
		v.addElement(cv);
		Attribute kurt = new Attribute("julien-norm-kurtosis-");
		v.addElement(kurt);
		Attribute p25 = new Attribute("julien-norm-percentile-percentile-25");
		v.addElement(p25);
		Attribute p75 = new Attribute("julien-norm-percentile-percentile-75");
		v.addElement(p75);

		Instances dataset = new Instances("Test", v, 0);
		Instance i = new Instance(dataset.numAttributes());
		i.setValue(mean, mean_norm);
		i.setValue(std, std_norm);
		i.setValue(min, min_norm);
		i.setValue(max, max_norm);
		i.setValue(range, range_norm);
		i.setValue(cv, cv_norm);
		i.setValue(kurt, kurt_norm);
		i.setValue(p25, percent25_norm);
		i.setValue(p75, percent75_norm);
		dataset.add(i);
		dataset.setClassIndex(0);
		double classe = 0.0;
		try {
			classe = cls_act.classifyInstance(dataset.instance(0));
		} catch (Exception e) {
			return;
		}

		long endLogTime = System.currentTimeMillis();
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Total Time to process data in dataAvailable() (without dataProduced()) : " + (endLogTime - startLogTime) + " ms.");

		dataProduced(new StreamElement(outputStructure, new Serializable[]{classe}, streamElements.get(streamElements.size() - 1).getTimeStamp()));

		if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE") || (boolean) Utils.getBuildConfigValue(StaticData.globalContext, "GPSPERFORMANCE")) {
			stopMethodTracing();
		}
	}

	@Override
	public DataField[] getOutputStructure(DataField[] in) {
		return outputStructure;
	}


}
