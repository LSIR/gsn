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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import org.bytedeco.javacpp.gsl;
import org.bytedeco.javacpp.gsl.gsl_interp_type;

import gsn.Main;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.monitoring.Monitorable;

public class StreamInterpolateJoinModel extends AbstractModel implements Monitorable{
	
	int historySize = 10000; //max size of segment + shift between the two streams
	int interpolateGap = 3000; //what gap to use for segmenting
	
	HashMap<Short,HashMap<String,LinkedList<Double>>> arrays = new HashMap<Short,HashMap<String,LinkedList<Double>>>(); //FIFO queues
	HashMap<Short,HashMap<String,double[]>> segments = new HashMap<Short,HashMap<String,double[]>>(); //interpolation segments
	HashMap<String,String> interpolation_types = new HashMap<String,String>();
	
	
	HashMap<Short,LinkedList<StreamElement>> toProcess = new HashMap<Short,LinkedList<StreamElement>>();
	
	int min_size = 2;
	HashMap<Short,Boolean> has_segment = new HashMap<Short,Boolean>();
	
	public StreamInterpolateJoinModel(){	
	}
	
	@Override
	public boolean initialize(){
		Main.getInstance().getToMonitor().add(this);
		return true;
	}
	

	@Override
	public synchronized StreamElement[] pushData(StreamElement streamElement, String origin) {
		Short type = (short)(((Short)streamElement.getData("station"))%100);
		if (!arrays.containsKey(type)){
			toProcess.put(type, new LinkedList<StreamElement>());
			arrays.put(type, new HashMap<String, LinkedList<Double>>());
			segments.put(type, new HashMap<String, double[]>());
			arrays.get(type).put("timed", new LinkedList<Double>());
			has_segment.put(type, false);
			for (String s:interpolation_types.keySet()){
				arrays.get(type).put(s, new LinkedList<Double>());
			}
		}
		if(origin.equalsIgnoreCase("A")){
			HashMap<String,LinkedList<Double>> a = arrays.get(type);
			
			if (a.get("timed").size() > 0 && a.get("timed").getFirst()==(double)streamElement.getTimeStamp()) return null; //Strictly increasing time
			//put into the buffers for interpolation
			for (int i = 0;i<streamElement.getFieldTypes().length;i++){
				if (a.containsKey(streamElement.getFieldNames()[i].toLowerCase())){
					a.get(streamElement.getFieldNames()[i].toLowerCase()).addFirst(((Number)streamElement.getData()[i]).doubleValue());
				}
			}
			a.get("timed").addFirst((double)streamElement.getTimeStamp());
			//keep size below the max
			if (a.get("timed").size() > historySize){
				for (String k : a.keySet()){
					a.get(k).removeLast();
				}
			}
			//clean according to the last item in todo list
			boolean modified = false;
			if (toProcess.get(type).size()>0){
				modified = removeOldSegments(type,new Long(toProcess.get(type).getFirst().getTimeStamp()).doubleValue());
			}
			if (modified || !has_segment.get(type)){// if modified or no existing segment, trying to find a new one
				has_segment.put(type, buildSegments(type));
			}
			//try to interpolate what is in the todo list
			return checkInterpolate(type,null);
		}else if(origin.equalsIgnoreCase("B")){
			//truncate the queues by removing too old segments
			if (removeOldSegments(type,new Long(streamElement.getTimeStamp()).doubleValue())){
				has_segment.put(type, buildSegments(type)); //find the next segment if possible
			}
			//try to interpolate for this element or add it to the todo list
			return checkInterpolate(type,streamElement);
		}
		return null;
	}
	

	private boolean removeOldSegments(Short type,double timed) {
		
		Iterator<Double> it = arrays.get(type).get("timed").descendingIterator();
		Double last = 0.0;
		Double cur = 0.0;
		int cut = 0; //how many to remove
		int ctr = 0;
		
		while (it.hasNext()){
			last = cur;
			cur = it.next();
			if(cur >= timed) break; //only check before this timepoint
			if (cur - last > interpolateGap){ //segments are defined by gaps in time
				cut = ctr;
			}
			ctr++;
		}
		if (cut>0){
			for (String k : arrays.get(type).keySet()){
				LinkedList<Double> l = arrays.get(type).get(k);
				for (int i=0;i<cut;i++){
					l.removeLast();
				}
			}
			return true;
		}else{
			return false;
		}
		
	}


	@Override
	public StreamElement[] query(StreamElement params) {
		
		Short type = (short)(((Short)params.getData("station"))%100);
		
		double q = new Long(params.getTimeStamp()).doubleValue();
		double[] x = segments.get(type).get("timed");
		
		//too small segment, don't interpolate
		if (x.length < min_size) return new StreamElement[]{null};
		
		//don't extrapolate for values between segments
		if(x[0] > q) return new StreamElement[]{null};
		if(x[x.length-1] < q)return new StreamElement[]{null};
		
		
		Serializable[] r = new Serializable[params.getData().length+interpolation_types.size()];
		int i = 0;
		for(;i<params.getData().length;i++){ //assumes that the first defined fields are the ones from B
			r[i] = params.getData()[i];
		}	
		//fill with default values
		for(int j=0;j<interpolation_types.size();j++){
			byte t = getOutputFields()[i+j].getDataTypeID();
			if (t == DataTypes.DOUBLE){
				r[i+j] = 0.0;
			}else if (t == DataTypes.FLOAT){
				r[i+j] = 0.0f;
			}else{
				r[i+j] = 0;
			}
		}
		StreamElement se = new StreamElement(getOutputFields(),r,params.getTimeStamp());

		for (String k : interpolation_types.keySet()){
			double[] y = segments.get(type).get(k);
			try {
				gsl.gsl_interp_type typ = (gsl_interp_type) gsl.class.getMethod(interpolation_types.get(k)).invoke(null);
				gsl.gsl_interp workspace = gsl.gsl_interp_alloc(typ,x.length);
				gsl.gsl_interp_accel acc = gsl.gsl_interp_accel_alloc();
			    gsl.gsl_interp_init(workspace, x, y, x.length); 
			    double val = gsl.gsl_interp_eval(workspace, x, y, q, acc);
			    if (se.getType(k) == DataTypes.FLOAT)
			        se.setData(k, (float)val);
			    else
			    	se.setData(k, val);
			    gsl.gsl_interp_free(workspace); 
			    gsl.gsl_interp_accel_free(acc);
		    
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new StreamElement[]{se};
	}

	@Override
	public void setParam(String k, String string) {
		//set the interpolation types
		if(k.startsWith("f_")){
			interpolation_types.put(k.substring(2).toLowerCase(), string);
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
		if(k.equalsIgnoreCase("interpolategap")){
			interpolateGap = Integer.parseInt(string);
		}
	}
	
	
	private StreamElement[] checkInterpolate(Short type,StreamElement element){
		//interpolation buffers are empty, pass
		if (arrays.get(type).get("timed").size() == 0) return null;
		
		if (element!=null)
			//the interpolation request is computable now
			//if a segment exists here, the element must be inside its time range
			if (has_segment.get(type)){
				StreamElement[] ses = query(element);
				if (ses[0] != null) return ses;
				else return null;
			}else{
		        toProcess.get(type).add(element);
		        return null;
			}
		ArrayList<StreamElement> r = new ArrayList<StreamElement>();
		int i = toProcess.get(type).size();
		while (i>0){
			StreamElement se = toProcess.get(type).removeFirst();
			i--;
			//only the latest element is guaranteed to be in the current segment
			//but none can be in another complete segment
			if (has_segment.get(type) && se.getTimeStamp() <= segments.get(type).get("timed")[segments.get(type).get("timed").length-1]){
				StreamElement ses = query(se)[0];
				if (ses != null) r.add(ses);
			}else{
				toProcess.get(type).add(se);
			}
		}
		return r.toArray(new StreamElement[r.size()]);
	}
	
	private boolean buildSegments(Short type){
		
		Iterator<Double> it = arrays.get(type).get("timed").descendingIterator();
		Double last = 0.0;
		Double cur = 0.0;
		int cut = 0; //how many to take
		int ctr = 0;
		
		while (it.hasNext()){
			last = cur;
			cur = it.next();
			if (cur - last > interpolateGap && ctr > 0){ //segments are defined by gaps in time
				cut = ctr;
				break; //find the first segment
			}
			ctr++;
		}
		if (cut>0){ //segment found
			for (String k : arrays.get(type).keySet()){
				double[] x = new double[cut];
				int i = cut -1;
				for(Double d :arrays.get(type).get(k)){
					x[i] = d.doubleValue();
					i--;
					if (i<0)break;
				}
				segments.get(type).put(k, x);
			}
			return true;
		}else{
			return false;
		}
	}


	@Override
	public Hashtable<String, Object> getStatistics() {
		Hashtable<String, Object> stat = new Hashtable<String, Object>();
		for (Short k :segments.keySet()){
		    stat.put("vs."+vs.getVirtualSensorConfiguration().getName().replaceAll("\\.", "_") +".Valuebuffer."+k+".size.value", arrays.get(k).get("timed").size());
		    if (segments.get(k).containsKey("timed")){
		        stat.put("vs."+vs.getVirtualSensorConfiguration().getName().replaceAll("\\.", "_") +".segment."+k+".size.value", segments.get(k).get("timed").length);
		    }else{
		    	stat.put("vs."+vs.getVirtualSensorConfiguration().getName().replaceAll("\\.", "_") +".segment."+k+".size.value", 0);
		    }
		}
		for (Short k :toProcess.keySet()){
		    stat.put("vs."+vs.getVirtualSensorConfiguration().getName().replaceAll("\\.", "_") +".Mappedbuffer."+k+".size.value", toProcess.get(k).size());
		}
		return stat;
	}
	
}
