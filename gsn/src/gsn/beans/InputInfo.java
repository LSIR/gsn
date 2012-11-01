package gsn.beans;

import java.util.ArrayList;
import java.util.Iterator;

public class InputInfo {
	private ArrayList<ProducerInfo> infoList = new ArrayList<ProducerInfo>();
	
	public InputInfo() {};
	
	public InputInfo(String producer, String info, boolean success) {
		infoList.add(new ProducerInfo(producer, info, success));
	}
	
	public void addInfo(String producer, String info, boolean success) {
		infoList.add(new ProducerInfo(producer, info, success));
	}
	
	public void addInfo(InputInfo inputInfo) {
		Iterator<ProducerInfo> iter = inputInfo.infoList.iterator();
		while (iter.hasNext()) {
			infoList.add(iter.next());
		}
	}
	
	public boolean hasAtLeastOneSuccess() {
		Iterator<ProducerInfo> iter = infoList.iterator();
		while (iter.hasNext()) {
			if (iter.next().success)
				return true;
		}
		return false;
	}
	
	protected ArrayList<ProducerInfo> getInfoList() {
		return infoList;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		Iterator<ProducerInfo> iter = infoList.iterator();
		while (iter.hasNext()) {
			str.append(iter.next()+"\n");
		}
		return str.toString();
	}
	
	private class ProducerInfo {
		protected String producer;
		protected String info;
		protected boolean success;
		
		protected ProducerInfo(String p, String i, boolean s) {
			producer = p;
			info = i;
			success = s;
		}
		
		public String toString() {
			return info + " (" + producer + ")";
		}
	}
}
