package gsn.http.rest;

import java.io.IOException;
import java.sql.SQLException;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.SQLValidator;
import gsn.utils.models.AbstractModel;

import org.apache.log4j.Logger;

public class ModelDistributionRequest implements DistributionRequest {

		private static transient Logger       logger     = Logger.getLogger ( DefaultDistributionRequest.class );

		private String query;

		private DeliverySystem deliverySystem;

		private VSensorConfig vSensorConfig;
		
		private AbstractModel modelClass;

	    private ModelDistributionRequest(DeliverySystem deliverySystem, VSensorConfig sensorConfig, String query, AbstractModel model) throws IOException, SQLException {
			this.deliverySystem = deliverySystem;
			vSensorConfig = sensorConfig;
			this.query = query;
			this.modelClass = model;
			DataField[] selectedColmnNames = SQLValidator.getInstance().extractSelectColumns(query,modelClass.getOutputFields());
			deliverySystem.writeStructure(selectedColmnNames);
		}

		public String toString() {
			return new StringBuilder("ModelDistributionRequest Request[[ Delivery System: ")
	                .append(deliverySystem.getClass().getName())
	                .append("],[Query:").append(query)
	                .append("],[model:")
	                .append(modelClass.getClass().getName())
	                .append("],[VirtualSensorName:")
	                .append(vSensorConfig.getName())
	                .append("]]").toString();
		}

	    public boolean deliverKeepAliveMessage() {
	        return deliverySystem.writeKeepAliveStreamElement();
	    }

		public boolean deliverStreamElement(StreamElement se) {		
			boolean success = deliverySystem.writeStreamElement(se);
			return success;
		}

		
		public String getQuery() {
			return query;
		}

		
		public VSensorConfig getVSensorConfig() {
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

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;

	        ModelDistributionRequest that = (ModelDistributionRequest) o;

	        if (deliverySystem != null ? !deliverySystem.equals(that.deliverySystem) : that.deliverySystem != null)
	            return false;
	        if (query != null ? !query.equals(that.query) : that.query != null) return false;
	        if (vSensorConfig != null ? !vSensorConfig.equals(that.vSensorConfig) : that.vSensorConfig != null)
	            return false;
	        if (modelClass != null ? modelClass.getClass() != that.modelClass.getClass() : that.modelClass != null)
	            return false;

	        return true;
	    }

	    @Override
	    public int hashCode() {
	        int result = query != null ? query.hashCode() : 0;
	        result = 31 * result + (deliverySystem != null ? deliverySystem.hashCode() : 0);
	        result = 31 * result + (vSensorConfig != null ? vSensorConfig.hashCode() : 0);
	        return result;
	    }
		

	@Override
	public long getStartTime() {
		return 0;
	}

	@Override
	public long getLastVisitedPk() {
		return 0;
	}


	public static ModelDistributionRequest create(DeliverySystem delivery,
			VSensorConfig vSensorConfig, String query, AbstractModel modelClass) throws IOException, SQLException {
		ModelDistributionRequest toReturn = new ModelDistributionRequest(delivery, vSensorConfig, query, modelClass);
		return toReturn;
	}

	@Override
	public AbstractModel getModel() {
		return modelClass;
	}

}
