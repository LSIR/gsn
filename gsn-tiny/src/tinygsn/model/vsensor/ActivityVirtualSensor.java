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
* File: gsn-tiny/src/tinygsn/model/vsensor/BridgeVirtualSensor.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.vsensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.Environment;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.SerializationHelper;


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

	
	private Classifier cls_act;
	private DataField[] outputStructure = new DataField[1];

	@Override
	public boolean initialize() {
		try {
			cls_act = (Classifier) SerializationHelper.read(Environment.getExternalStorageDirectory().getAbsolutePath()
	                + "/Android/data/tinygsn/activity.model");
		} catch (Exception e) {
			return false;
		}
		outputStructure[0] = new DataField("activity",DataTypes.DOUBLE);
		
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {}
	
	@Override
	public void dataAvailable(String inputStreamName, ArrayList<StreamElement> streamElements) {
		
		double[] x = new double[streamElements.size()];
		double[] y = new double[x.length];
		double[] z = new double[x.length];
		
		double mean_x = 0;
		double mean_y = 0;
		double mean_z = 0;
		double std_x = 0;
		double std_y = 0;
		double std_z = 0;
		double min_x = Double.MAX_VALUE;
		double min_y = Double.MAX_VALUE;
		double min_z = Double.MAX_VALUE;
		double max_x = Double.MIN_VALUE;
		double max_y = Double.MIN_VALUE;
		double max_z = Double.MIN_VALUE;
		
		for (int i=0;i<x.length;i++){
			Serializable[] d = streamElements.get(i).getData();
			x[i] = (Double)d[0];
			y[i] = (Double)d[1];
			z[i] = (Double)d[2];
			mean_x += x[i];
			mean_y += y[i];
			mean_z += z[i];
			std_x += x[i] * x[i];
			std_y += y[i] * y[i];
			std_z += z[i] * z[i];
			min_x = x[i] < min_x ? x[i] : min_x;
			min_y = y[i] < min_y ? y[i] : min_y;
			min_z = z[i] < min_z ? z[i] : min_z;
			max_x = x[i] < max_x ? x[i] : max_x;
			max_y = y[i] < max_y ? y[i] : max_y;
			max_z = z[i] < max_z ? z[i] : max_z;
		}
		mean_x /= x.length;
		mean_y /= y.length;
		mean_z /= z.length;
		
		std_x = Math.sqrt(std_x / x.length - mean_x * mean_x);
		std_y = Math.sqrt(std_y / y.length - mean_y * mean_y);
		std_z = Math.sqrt(std_z / z.length - mean_z * mean_z);
		
		double range_x = max_x - min_x;
		double range_y = max_y - min_y;
		double range_z = max_z - min_z;

		double cv_x = std_x / mean_x;
		double cv_y = std_y / mean_y;
		double cv_z = std_z / mean_z;
		
		double kurt_x = 0;
		double kurt_y = 0;
		double kurt_z = 0;
		
        for (int i=0;i<x.length;i++){
        	kurt_x += Math.pow(x[i]-mean_x, 4);
        	kurt_y += Math.pow(y[i]-mean_y, 4);
        	kurt_z += Math.pow(z[i]-mean_z, 4);
        }
        
        kurt_x = (kurt_x / x.length) / Math.pow(std_x, 4);
        kurt_y = (kurt_y / y.length) / Math.pow(std_y, 4);
        kurt_z = (kurt_x / z.length) / Math.pow(std_z, 4);
        
        Arrays.sort(x);
        Arrays.sort(y);
        Arrays.sort(z);
        
        double median_x = x.length % 2 == 1 ? x[(x.length - 1)/2] : x[(x.length)/2] + x[(x.length)/2 - 1] / 2.0;
        double median_y = y.length % 2 == 1 ? y[(y.length - 1)/2] : y[(y.length)/2] + y[(y.length)/2 - 1] / 2.0;
        double median_z = z.length % 2 == 1 ? z[(z.length - 1)/2] : z[(z.length)/2] + z[(z.length)/2 - 1] / 2.0;
        
        double percent25_x = x[(int)Math.round(x.length/4.0)];
        double percent25_y = y[(int)Math.round(y.length/4.0)];
        double percent25_z = z[(int)Math.round(z.length/4.0)];
        
        double percent75_x = x[(int)Math.round(3.0*x.length/4)];
        double percent75_y = y[(int)Math.round(3.0*y.length/4)];
        double percent75_z = z[(int)Math.round(3.0*z.length/4)];
        
        double[] vector = new double[]{mean_x,std_x,min_x,max_x,range_x,cv_x,kurt_x,median_x,percent25_x,percent75_x,
        		                       mean_y,std_y,min_y,max_y,range_y,cv_y,kurt_y,median_y,percent25_y,percent75_y,
        		                       mean_z,std_z,min_z,max_z,range_z,cv_z,kurt_z,median_z,percent25_z,percent75_z};
		Instance i = new Instance(1, vector);
		double classe = 0.0;
		try {
			classe = cls_act.classifyInstance(i);
		} catch (Exception e) {
			return;
		}
		dataProduced(new StreamElement(outputStructure, new Serializable[]{classe}, streamElements.get(streamElements.size()-1).getTimeStamp()));
	}
	
	@Override
	public DataField[] getOutputStructure(DataField[] in){
		return outputStructure;
	}
	

}
