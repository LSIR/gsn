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
* File: src/gsn/utils/models/StreamInterpolateJoinModel.java
*
* @author Julien Eberle
*
*/


package gsn.utils.models;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.bytedeco.javacpp.gsl;
import org.bytedeco.javacpp.gsl.gsl_interp_type;

import ucar.nc2.stream.NcStreamProto.DataType;

import gsn.beans.StreamElement;

public class StreamInterpolateJoinModel extends AbstractModel {
	
	int historySize = 10;
	
	HashMap<String,LinkedList<Double>> arrays = new HashMap<String, LinkedList<Double>>(); //FIFO queues
	HashMap<String,String> interpolation_types = new HashMap<String,String>();
	
	LinkedList<StreamElement> toProcess = new LinkedList<StreamElement>();
	
	int min_size = 2;
	
	public StreamInterpolateJoinModel(){
		arrays.put("timed", new LinkedList<Double>());
	}
	

	@Override
	public synchronized StreamElement[] pushData(StreamElement streamElement, String origin) {
		if(origin.equalsIgnoreCase("A")){
			for (int i = 0;i<streamElement.getFieldTypes().length;i++){
				if (arrays.containsKey(streamElement.getFieldNames()[i])){
					arrays.get(streamElement.getFieldNames()[i]).addFirst(((Double)streamElement.getData()[i]));
				}
			}
			arrays.get("timed").addFirst((double)streamElement.getTimeStamp());
			
			if (arrays.get("timed").size() > historySize){
				for (String k : arrays.keySet()){
					arrays.get(k).removeLast();
				}
			}
			return checkInterpolate(null);
		}else if(origin.equalsIgnoreCase("B")){
			return checkInterpolate(streamElement);
		}
		return null;
	}
	

	@Override
	public StreamElement[] query(StreamElement params) {
		
		double q = (double)params.getTimeStamp();
		int size = arrays.get("timed").size();
		double[] x = new double[size];
		int i = 0;
		for(Double d :arrays.get("timed")){
			x[i] = d.doubleValue();
		}
		
		Serializable[] r = new Serializable[params.getData().length+interpolation_types.size()];
		for(;i<params.getData().length;i++){
			r[i] = params.getData()[i];
		}		
		for(int j=0;j<interpolation_types.size();j++){
			byte t = getOutputFields()[i].getDataTypeID();
			if (t == DataType.DOUBLE_VALUE){
				r[i+j] = 0.0;
			}else if (t == DataType.FLOAT_VALUE){
				r[i+j] = 0.0f;
			}else{
				r[i+j] = 0;
			}
		}
		StreamElement se = new StreamElement(getOutputFields(),r,params.getTimeStamp());

		for (String k : interpolation_types.keySet()){
			double[] y = new double[size];
			int j = 0;
			for(Double d :arrays.get(k)){
				y[j] = d.doubleValue();
			}
			try {
				gsl.gsl_interp_type typ = (gsl_interp_type) gsl.class.getMethod(interpolation_types.get(k)).invoke(null);
				gsl.gsl_interp workspace = gsl.gsl_interp_alloc(typ,size);
			    gsl.gsl_interp_init(workspace, x, y, size);
			    double val = gsl.gsl_interp_eval(workspace, x, y, q, null);
			    se.setData(k, val);
			    gsl.gsl_interp_free(workspace); 
		    
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new StreamElement[]{se};
	}

	@Override
	public void setParam(String k, String string) {
		if(k.startsWith("f_")){
			arrays.put(k.substring(2), new LinkedList<Double>());
			interpolation_types.put(k.substring(2), string);
			try {
				gsl.gsl_interp_type typ = (gsl_interp_type) gsl.class.getMethod(string).invoke(null);
				int m = typ.min_size();
				if (m>min_size) min_size = m;
				if (m>historySize) historySize = m;
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
		}
		if(k.equalsIgnoreCase("historysize")){
			historySize = Integer.parseInt(string);
		}
	}
	
	
	private StreamElement[] checkInterpolate(StreamElement element){
		if (arrays.get("timed").size() == 0) return null;
		if (element!=null)
			if (element.getTimeStamp() <= arrays.get("timed").getFirst() && arrays.get("timed").size()>= min_size){
				return query(element);
			}else{
		        toProcess.add(element);
		        return null;
			}
		ArrayList<StreamElement> r = new ArrayList<StreamElement>();
		int i = toProcess.size();
		while (i>0){
			StreamElement se = toProcess.removeFirst();
			i--;
			if (se.getTimeStamp() <= arrays.get("timed").getFirst() && arrays.get("timed").size()>= min_size){
				StreamElement ses = query(se)[0];
				if (ses != null) r.add(ses);
			}else{
				toProcess.add(se);
			}
		}
		return r.toArray(new StreamElement[r.size()]);
	}
	
}
