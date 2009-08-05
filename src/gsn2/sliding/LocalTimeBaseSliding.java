package gsn2.sliding;

import gsn.beans.StreamElement;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Timer;

public class LocalTimeBaseSliding implements ActionListener,SlidingHandler{

	private int size;
	    private Timer timer;// TODO: replacing with Quartz
	    ArrayList<SlidingListener> listeners = new ArrayList<SlidingListener>();
	    
	    public LocalTimeBaseSliding (int slidingPeriodInMSec) {
	        this.size = slidingPeriodInMSec;
	        timer = new Timer(size, this);
	        timer.start();
	    }

	    public void postData(StreamElement se) {
	    	
	    }
	    
	    public void addListener(SlidingListener listener) {
	    	if (listeners.contains(listener))
	    		return;
	    	listeners.add(listener);
	    }
	    
	    public void removeListener(SlidingListener listener) {
	    	listeners.remove(listener);
	    }
	    
	    

	    public void reset() {
	        timer.stop();
	        timer = new Timer(size, this);
	        timer.start();
	    }

	    public void actionPerformed(ActionEvent e) {
	    	long currentTimeMillis = System.currentTimeMillis();
	    	for (SlidingListener listener:listeners) 
				listener.slide(currentTimeMillis);
	    }
}
