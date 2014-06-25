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
import java.util.Iterator;
import java.util.LinkedList;

import gsn.beans.StreamElement;

public class StreamInterpolateJoinModel extends AbstractModel {
	
	int historySize = 10;
	LinkedList<StreamElement> buffer = new LinkedList<StreamElement>();
	LinkedList<StreamElement> toProcess = new LinkedList<StreamElement>();


	@Override
	public synchronized StreamElement[] pushData(StreamElement streamElement, String origin) {
		if(origin.equalsIgnoreCase("A")){
			buffer.addFirst(streamElement);
			if (buffer.size() > historySize){
				buffer.removeLast();
			}
			return checkInterpolate(null);
		}else if(origin.equalsIgnoreCase("B")){
			return checkInterpolate(streamElement);
		}
		return null;
	}

	@Override
	public StreamElement[] query(StreamElement params) {
		Iterator<StreamElement> it = buffer.descendingIterator();
		if (params.getTimeStamp() <= buffer.getLast().getTimeStamp())
			return new StreamElement[]{merge(params,buffer.getLast(),buffer.getLast())};
		if (params.getTimeStamp() >= buffer.getFirst().getTimeStamp())
			return new StreamElement[]{merge(params,buffer.getFirst(),buffer.getFirst())};
		StreamElement after = it.next();
		StreamElement before = after;
		while(it.hasNext()){
			before = it.next();
			if (params.getTimeStamp() >= before.getTimeStamp()) break;
			after = before;
		}
		return new StreamElement[]{merge(params,before,after)};
	}

	@Override
	public void setParam(String k, String string) {
		if(k.equalsIgnoreCase("historysize")){
			historySize = Integer.parseInt(string);
		}
	}
	
	
	private StreamElement[] checkInterpolate(StreamElement element){
		if (buffer.size() == 0) return null;
		if (element!=null)
			if (element.getTimeStamp() <= buffer.getFirst().getTimeStamp()){
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
			if (se.getTimeStamp() <= buffer.getFirst().getTimeStamp()){
				StreamElement ses = query(se)[0];
				if (ses != null) r.add(ses);
			}else{
				toProcess.add(se);
			}
		}
		return r.toArray(new StreamElement[r.size()]);
	}
	
	private StreamElement merge(StreamElement data, StreamElement before, StreamElement after){
		Serializable[] r = new Serializable[data.getData().length+after.getData().length];
		double ratio = 1;
		if (after.getTimeStamp() != before.getTimeStamp())
		    ratio = (data.getTimeStamp() - before.getTimeStamp()) * 1.0 / (after.getTimeStamp() - before.getTimeStamp());
		int i=0;
		for(;i<data.getData().length;i++){
			r[i] = data.getData()[i];
		}
		for(int j=0;j<after.getData().length;j++){
			if (after.getFieldTypes()[j] == 5){
				r[i+j] = ((Double)after.getData()[j]) * ratio + ((Double)before.getData()[j]) * (1-ratio);
			}else{
				r[i+j] = before.getData()[j];
			}
		}
		return new StreamElement(getOutputFields(),r,data.getTimeStamp());
	}
}
