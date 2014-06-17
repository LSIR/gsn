package tinygsn.beans;

import java.util.ArrayList;

public interface QueueListener {
	public void notifyMe(ArrayList<StreamElement> s);
}
