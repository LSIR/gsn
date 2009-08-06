package gsn.http.rest;

import gsn.beans.StreamElement;
import gsn.beans.VSFile;

public interface DistributionRequest {

	public abstract boolean deliverStreamElement(StreamElement se);

	public abstract long getLastVisitedTime();

	public abstract String getQuery();

	public abstract VSFile getVSensorConfig();

	public abstract void close();

	public abstract boolean isClosed();
	
	public abstract DeliverySystem getDeliverySystem();

}