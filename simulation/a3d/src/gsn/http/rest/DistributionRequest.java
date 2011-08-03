package gsn.http.rest;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

public interface DistributionRequest {

	public abstract boolean deliverStreamElement(StreamElement se);

    public boolean deliverKeepAliveMessage();

    public abstract long getStartTime();

    public abstract long getLastVisitedPk();

    public abstract String getQuery();

    public abstract VSensorConfig getVSensorConfig();

    public abstract void close();

    public abstract boolean isClosed();

    public abstract DeliverySystem getDeliverySystem();

}