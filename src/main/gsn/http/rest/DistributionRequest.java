package gsn.http.rest;

import gsn.beans.StreamElement;
import gsn2.conf.OperatorConfig;

public interface DistributionRequest {

	public abstract boolean deliverStreamElement(StreamElement se);

	public abstract long getLastVisitedTime();

	public abstract String getQuery();

	public abstract OperatorConfig getVSensorConfig();

	public abstract void close();

	public abstract boolean isClosed();
	
	public abstract DeliverySystem getDeliverySystem();

}