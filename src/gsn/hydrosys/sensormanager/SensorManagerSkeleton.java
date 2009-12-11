/**
 * SensorManagerSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:35 LKT)
 */
package gsn.hydrosys.sensormanager;

import java.io.File;

import gsn.Main;
import gsn.VSensorLoader;

import org.apache.log4j.Logger;

/**
 * SensorManagerSkeleton java skeleton for the axisService
 */
public class SensorManagerSkeleton {
	
	private static final transient Logger logger = Logger.getLogger(SensorManagerSkeleton.class);

	/**
	 * Auto generated method signature
	 * 
	 * @param registerQuery
	 */

	public gsn.hydrosys.sensormanager.RegisterQueryResponse registerQuery(gsn.hydrosys.sensormanager.RegisterQuery registerQuery) {
		gsn.hydrosys.sensormanager.RegisterQueryResponse response = new gsn.hydrosys.sensormanager.RegisterQueryResponse();
		String message;
		boolean status = true;
		message = "Created " + getMessage(registerQuery.getQueryName());
		try {
			VSensorLoader.getInstance(Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY).loadVirtualSensor(
					createVSConfigurationFileContent(registerQuery), 
					registerQuery.getQueryName()
				);			
		} catch (Exception e) {
			message = "Unable to create " + getMessage(registerQuery.getQueryName()) + "\nCause " + e.getMessage();
			status = false;
		}

		logger.info(message);
		response.setStatus(status);
		response.setMessage(message);
		return response;
	}

	/**
	 * Auto generated method signature
	 * 
	 * @param unregisterQuery
	 */

	public gsn.hydrosys.sensormanager.UnregisterQueryResponse unregisterQuery(gsn.hydrosys.sensormanager.UnregisterQuery unregisterQuery) {
		gsn.hydrosys.sensormanager.UnregisterQueryResponse response = new gsn.hydrosys.sensormanager.UnregisterQueryResponse();
		File vsConfigurationFile = new File(VSensorLoader.getVSConfigurationFilePath(unregisterQuery.getQueryName()));
		String message;
		if ( ! vsConfigurationFile.delete()) {
			message = "Failed to delete " + getMessage(unregisterQuery.getQueryName());
			response.setStatus(false);
		}
		else {
			message = "Deleted " + getMessage(unregisterQuery.getQueryName());
			response.setStatus(true);
		}
		
		logger.info(message);	
		response.setMessage(message);
		return response;
	}
	
	private String createVSConfigurationFileContent(gsn.hydrosys.sensormanager.RegisterQuery registerQuery){
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<virtual-sensor name=\"" + registerQuery.getQueryName() + "\" priority=\"10\" >\n");
		sb.append("		<processing-class>\n");
		sb.append("			<class-name>gsn.vsensor.BridgeVirtualSensor</class-name>\n");
		sb.append("			<init-params/>\n");
		sb.append("			<output-structure>\n");
		gsn.beans.xsd.DataField df;
		for (int i = 0 ; i < registerQuery.getOutputStructure().length ; i++) {
			df = registerQuery.getOutputStructure()[i];
			sb.append("			<field name=\"" + df.getName() + "\" type=\"" + df.getType() + "\"/>\n");
		}		
		sb.append("			</output-structure>\n");
		sb.append("		</processing-class>\n");
		sb.append("		<description>this VS implements the registered query: memquery</description>\n");
		sb.append("		<life-cycle pool-size=\"10\" />\n");
		sb.append("		<addressing>\n");
		sb.append("		</addressing>\n");
		sb.append("		<storage />\n");
		sb.append("		<streams>\n");
		sb.append("			<stream name=\"data\">\n");
		String vsname;
		for (int i = 0 ; i < registerQuery.getVsnames().length ; i++) {
			vsname = registerQuery.getVsnames()[i];
			sb.append("			<source alias=\"" + vsname + "\" storage-size=\"1\" sampling-rate=\"1\">\n");
			sb.append("				<address wrapper=\"local\">\n");
			sb.append("					<predicate key=\"NAME\">" + vsname + "</predicate>\n");
			sb.append("				</address>\n");
			sb.append("				<query>select * from wrapper</query>\n");
			sb.append("			</source>\n");
		}
		sb.append("				<query>" + registerQuery.getQuery() + "</query>\n");
		sb.append("			</stream>\n");
		sb.append("		</streams>\n");
		sb.append("</virtual-sensor>\n");
		return sb.toString();
	}
	
	private String getMessage(String queryName) {
		return "the VS file (" + VSensorLoader.getVSConfigurationFilePath(queryName) + ") for the registered query: " + queryName;
	}
	
}
