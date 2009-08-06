package gsn2.rate;

import gsn.beans.StreamElement;

public interface RateHandler {
	public boolean canProceed(StreamElement se);
}
