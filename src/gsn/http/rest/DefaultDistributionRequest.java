package gsn.http.rest;

import gsn.beans.StreamElement;
import gsn.beans.VSFile;
import gsn.storage.SQLValidator;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class DefaultDistributionRequest implements DistributionRequest {

	private static transient Logger       logger     = Logger.getLogger ( DefaultDistributionRequest.class );

	private long lastVisitedTime;

	private String query;

	private DeliverySystem deliverySystem;

	private VSFile vSensorConfig;

	private DefaultDistributionRequest(DeliverySystem deliverySystem, VSFile sensorConfig, String query, long lastVisitedTime) throws IOException, SQLException {
		this.deliverySystem = deliverySystem;
		vSensorConfig = sensorConfig;
		this.query = query;
		this.lastVisitedTime = lastVisitedTime;
		deliverySystem.writeStructure(SQLValidator.getInstance().extractSelectColumns(query));
	}

	public static DefaultDistributionRequest create(DeliverySystem deliverySystem, VSFile sensorConfig,String query, long lastVisitedTime) throws IOException, SQLException {
		DefaultDistributionRequest toReturn = new DefaultDistributionRequest(deliverySystem,sensorConfig,query,lastVisitedTime);
		return toReturn;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("DefaultDistributionRequest Request[[ Delivery System: ").append(deliverySystem.getClass().getName()).append("],[Query:").append(query).append("],[StartTime:").append(lastVisitedTime).append("],[VirtualSensorName:").append(vSensorConfig.getName()).append("]]");
		return sb.toString();
	}

	public boolean deliverStreamElement(StreamElement se)  {		
		boolean success = deliverySystem.writeStreamElement(se);
//		boolean success = true;
		if (success)
			lastVisitedTime=se.getTimeStamp();
		return success;
	}


	public long getLastVisitedTime() {
		return lastVisitedTime;
	}

	
	public String getQuery() {
		return query;
	}

	
	public VSFile getVSensorConfig() {
		return vSensorConfig;
	}

	
	public void close() {
		deliverySystem.close();
	}

	
	public boolean isClosed() {
		return deliverySystem.isClosed();
	}

	public DeliverySystem getDeliverySystem() {
		return deliverySystem;
	}
}
	
