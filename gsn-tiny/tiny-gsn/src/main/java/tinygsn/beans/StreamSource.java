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
* File: gsn-tiny/src/tinygsn/beans/Queue.java
*
* @author Do Ngoc Hoan
*/

package tinygsn.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import tinygsn.model.wrappers.AbstractWrapper;


public class StreamSource implements Serializable{
	
	private static final long serialVersionUID = 7791682194678539871L;

	public static final String[] AGGREGATOR = { "Average", "Max", "Min", "Window" };

	//properties
	private int id = 0;
	private int step = 1;
	private int windowSize = 1;
	private boolean timeBased = false;
	private int aggregator = 0;
	
	//references
	private transient AbstractWrapper wrapper;
	private transient InputStream inputStream;
	
	//internals
	private Date lastTimeAdded;
	private ArrayList<StreamElement> values = new ArrayList<StreamElement>();
	
	public StreamSource(){}
	
	public StreamSource(int windowSize, int step, boolean timeBased, int aggregator){
		setStep(step);
		setWindowSize(windowSize);		
		setTimeBased(timeBased);
		setAggregator(aggregator);
	}
	
	/**
	 * Assume step <= windowSize
	 * We keep the Queue's size is double of window size
	 */
	public boolean isFull(){
		if (isTimeBased()){
			return values.get(values.size()-1).getTimeStamp() - values.get(0).getTimeStamp() >= windowSize * 1000;
		}
		else{
			return values.size() >= windowSize;
		}
	}

	public boolean isEmpty(){
		return values.size() == 0;
	}
	
	public synchronized void add(StreamElement element){
		values.add(element);
		lastTimeAdded = new Date();
		if (this.isFull()){
			notifyInputStream(values);
			moveToNextStep();
		}
	}
	
	private synchronized void moveToNextStep(){
		if (isTimeBased()){
			long ref = values.get(0).getTimeStamp() + step*1000;
			while (!isEmpty() && values.get(0).getTimeStamp() < ref){
				values.remove(0);
			}
		}else{
			for (int i = 0; i < step; i++){
				values.remove(0);
			}
		}
	}
	
	public void notifyInputStream(ArrayList<StreamElement> data) {
		// Use Aggregator to process data s
		StreamElement se = data.get(0);

		if (inputStream == null) {
			return;
		}
		switch (aggregator) {
		case 0:
			// Average
			for (int i = 1; i < data.size(); i++) {
				for (int j = 0; j < se.getFieldNames().length; j++) {
					StreamElement se_i = data.get(i);
					se.setData(j, getDouble(se.getData()[j]) + getDouble(se_i.getData()[j]));
				}
			}
			for (int j = 0; j < se.getFieldNames().length; j++) {
				se.setData(j, getDouble(se.getData()[j]) / data.size());
			}
		case 1:
			// Max
			for (int i = 1; i < data.size(); i++) {
				if (getDouble(data.get(i).getData()[0]) > getDouble(se.getData()[0])) {
					se = data.get(i);
				}
			}
		case 2:
			// Min
			for (int i = 1; i < data.size(); i++) {
				if (getDouble(data.get(i).getData()[0]) < getDouble(se.getData()[0])) {
					se = data.get(i);
				}
			}
		case 3:
			// No aggregation
			inputStream.getVirtualSensor().dataAvailable(getWrapper().getWrapperName(),data);
			return;
		}	
		inputStream.getVirtualSensor().dataAvailable(getWrapper().getWrapperName(),se);
		
	}

	private double getDouble(Serializable s){
		double d = ((Number) s).doubleValue() ;
		return d;
	}
	
	/**
	 * Get the first windowSize elements
	 * @return
	 */
	public ArrayList<StreamElement> getWindowValues(){
		ArrayList<StreamElement> window = new ArrayList<StreamElement>();
		if (isTimeBased()){
			int i = 0;
			long ref = values.get(0).getTimeStamp() + windowSize * 1000;
			while (i < values.size() && values.get(i).getTimeStamp() < ref){
				window.add(values.get(i));
			}
		}else{
			int w = windowSize;
			if (w < values.size()) w = values.size();
			for (int i = 0; i < w; i++){
				window.add(values.get(i));
			}
		}
		return window;
	}
	
	public void dispose(){
		wrapper.unregisterListener(this);
	}
	
	public ArrayList<StreamElement> getAllValues(){
		return values;
	}
	
	public String toString(){
		String s = "";
		for (StreamElement i: getAllValues()){
			s += i.toString() + " ";
		}
		return s;
	}
	
	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public Date getLastTimeAdded() {
		return lastTimeAdded;
	}

	public boolean isTimeBased() {
		return timeBased;
	}

	public void setTimeBased(boolean timeBased) {
		this.timeBased = timeBased;
	}
	
	public int getAggregator() {
		return aggregator;
	}

	public void setAggregator(int aggregator) {
		this.aggregator = aggregator;
	}

	public AbstractWrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(AbstractWrapper wrapper) {
		this.wrapper = wrapper;
		wrapper.registerListener(this);
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
