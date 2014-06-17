package tinygsn.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Queue_Template<T>{

	private int step = 1;
	private int timeInterval = 1000; //intended to use in case the step is time
	private int windowSize = 1;
	private Date lastTimeAdded;
	private ArrayList<T> values = new ArrayList<T>();
	
	List<QueueListener> listeners = new ArrayList<QueueListener>() ; 
	// add elements of a concrete QueueListener, which implements QueueListener 
	
	public Queue_Template(){
		
	}
	
	public Queue_Template(int step, int windowSize){
		this.setStep(step);
		this.setWindowSize(windowSize);		
	}
	
	/**
	 * Assume step <= windowSize
	 * We keep the Queue's size is double of window size
	 */
	public boolean isTooFull(){
		return values.size() > windowSize * 2;
	}

	public boolean isEmpty(){
		return values.size() == 0;
	}
	
	public synchronized Queue_Template<T> add(T element){
		values.add(element);
		lastTimeAdded = new Date();
		
		if (this.isTooFull()){
			values.remove(0);
		}
		
		if (values.size() >= step){
			for (QueueListener l: listeners){
//				l.notifyMe(s);
			}
		}
		
		return this;
	}
	
	public synchronized Queue_Template<T> moveToNextStep(){
		for (int i = 0; i < step; i++){
			values.remove(0);
		}
		return this;
	}
	
	/**
	 * Get the first windowSize elements
	 * @return
	 */
	public ArrayList<T> getWindowValues(){
		int w = windowSize;
		if (w < values.size()) w = values.size();
		ArrayList<T> window = new ArrayList<T>();
		for (int i = 0; i < w; i++){
			window.add(values.get(i));
		}
		return window;
	}
	
	public ArrayList<T> getAllValues(){
		return values;
	}
	
	public String toString(){
		String s = "";
		for (T i: getAllValues()){
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
	
	/******************************************************
	 * TEST
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Queue_Template<Integer> q = new Queue_Template<Integer>(2, 3);
		System.out.println("Started");
		for (int i = 0; i < 30; i++){
			q.add(i);
			System.out.println(q.toString());
		}
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
