package gsn2.rate;

import gsn.beans.StreamElement;

public class DefaultRateHandler implements RateHandler {
	
	private double rate;

	public DefaultRateHandler(double rate) {
		this.rate = rate;
	}

	public boolean canProceed(StreamElement se) {
		return true;
	}
	
	
}
