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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.util.Log;

public class Queue{

	private int step = 1;
	private int timeInterval = 1000; //intended to use in case the step is time
	private int windowSize = 1;
	private Date lastTimeAdded;
	private ArrayList<StreamElement> values = new ArrayList<StreamElement>();
	
	List<QueueListener> listeners = new ArrayList<QueueListener>() ; 
	// add elements of a concrete QueueListener, which implements QueueListener 
	
	public Queue(){
		
	}
	
	public Queue(int windowSize, int step){
		this.setStep(step);
		this.setWindowSize(windowSize);		
	}
	
	/**
	 * Assume step <= windowSize
	 * We keep the Queue's size is double of window size
	 */
	public boolean isFull(){
		return values.size() >= windowSize;
	}

	public boolean isEmpty(){
		return values.size() == 0;
	}
	
	public synchronized Queue add(StreamElement element){
		values.add(element);
		lastTimeAdded = new Date();
		
		if (this.isFull()){
			for (QueueListener l: listeners){
				l.notifyMe(values);
				moveToNextStep();
			}
		}
		
		return this;
	}
	
	public synchronized Queue moveToNextStep(){
		for (int i = 0; i < step; i++){
			values.remove(0);
		}
		return this;
	}
	
	/**
	 * Get the first windowSize elements
	 * @return
	 */
	public ArrayList<StreamElement> getWindowValues(){
		int w = windowSize;
		if (w < values.size()) w = values.size();
		ArrayList<StreamElement> window = new ArrayList<StreamElement>();
		for (int i = 0; i < w; i++){
			window.add(values.get(i));
		}
		return window;
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
	
	public void registerListener(QueueListener listener){
		this.listeners.add(listener);
	}
	
	public void unRegisterListener(QueueListener listener){
		this.listeners.remove(listener);
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

	public int getInterval() {
		return timeInterval;
	}

	public void setInterval(int interval) {
		this.timeInterval = interval;
	}

}
