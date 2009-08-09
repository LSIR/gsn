package gsn.http.rest;

import gsn.DataDistributer;
import gsn.ManualDataConsumer;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.storage.SQLUtils;
import gsn.storage.SQLValidator;
import gsn.utils.Helpers;
import gsn.wrappers.Wrapper;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.joda.time.format.ISODateTimeFormat;
import gsn2.conf.OperatorConfig;

public class LocalDeliveryWrapper implements Wrapper , DeliverySystem,ManualDataConsumer{

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private  final String CURRENT_TIME = ISODateTimeFormat.dateTime().print(System.currentTimeMillis());

	private static transient Logger                  logger           = Logger.getLogger( LocalDeliveryWrapper.class );

	private OperatorConfig vSensorConfig;

	public OperatorConfig getVSensorConfig() {
		return vSensorConfig;
	}

	private DataField[] structure;

	private DefaultDistributionRequest distributionRequest;

	private boolean isActive = true;

	public String getWrapperName() {
		return "Local-wrapper";
	}

	public LocalDeliveryWrapper(WrapperConfig conf, DataChannel channel) throws SQLException, IOException {
		this.conf = conf;
		this.dataChannel= channel;

		String query = conf.getParameters().getValue("query");

		String vsName = conf.getParameters().getValue( "name" );
		String startTime = conf.getParameters().getValueWithDefault("start-time",CURRENT_TIME );

		if (query==null && vsName == null) 
			throw new RuntimeException("For using local-wrapper, either >query< or >name< parameters should be specified"); 

		if (query == null) 
			query = "select * from "+vsName;

		long lastVisited;

		try {
			lastVisited = Helpers.convertTimeFromIsoToLong(startTime);
		}catch (Exception e) {
			throw new RuntimeException("Problem in parsing the start-time parameter, the provided value is:"+startTime+" while a valid input is:"+CURRENT_TIME,e);
		}

		vsName = SQLValidator.getInstance().validateQuery(query);
		if(vsName==null) //while the other instance is not loaded.
			throw new RuntimeException("The local-virtual sensor doesn't exist.");

		query = SQLUtils.newRewrite(query, vsName, vsName.toLowerCase()).toString();
		logger.debug("Local wrapper request received for: "+vsName);

			vSensorConfig = null;//Mappings.getConfig(vsName);
			distributionRequest = DefaultDistributionRequest.create(this, vSensorConfig, query, lastVisited);
			// This call MUST be executed before adding this listener to the data-distributer because distributer checks the isClose method before flushing.
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("LocalDistributionReq => [" ).append(distributionRequest.getQuery()).append(", Start-Time: ").append(new Date(distributionRequest.getLastVisitedTime())).append("]");
		return sb.toString();
	}

	public void writeStructure(DataField[] fields) throws IOException {
		this.structure=fields;

	}

	public DataField[] getOutputFormat() {
		return structure;
	}

	public boolean writeStreamElement(StreamElement se) {
		dataChannel.write(se);
		logger.debug("wants to deliver stream element:"+ se.toString());
		return true;
	}

	public void dispose() {

	}

	public void start() {
		DataDistributer localDistributer = DataDistributer.getInstance(LocalDeliveryWrapper.class);
		localDistributer.addListener(this.distributionRequest);
	}

	public void stop() {
		isActive = false;
	}
	public void close() {
		logger.warn("Closing a local delivery.");
	}

	public boolean isClosed() {
		return !isActive ;
	}

	public boolean handleExternalInput(String action, String[] paramNames,Serializable[] paramValues) {
//		ProcessingOperator vs;
//		try {
//			vs = Mappings.getVSensorInstanceByVSName( vSensorConfig.getName( ) ).borrowVS( );
//		} catch ( VirtualSensorInitializationFailedException e ) {
//			logger.warn("Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
//			return false;
//		}
//		boolean toReturn = vs.handleExternalInput( action , paramNames , paramValues );
//		return toReturn;
		return false;
	}


}
