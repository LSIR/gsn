package gsn.msr.sensormap;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.utils.KeyValueImp;

import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoginToMSRSense {

	public static int REGISTER_SENSOR_ERROR_NOT_CREATED = 0;
	public static int REGISTER_SENSOR_OK_CREATED_NEW = 1;
	public static int REGISTER_SENSOR_ERROR_ALREADY_EXISTS = 2;
	public static int DELETE_SENSOR_OK = 1;
	public static int DELETE_SENSOR_ERROR_DOESNT_EXIST = 0;

	private static transient Logger logger = Logger
			.getLogger(LoginToMSRSense.class);
	public static final String DEFAULT_GSN_LOG4J_PROPERTIES = "conf/log4j.properties";

	private static ArrayList<String> dataTypeCache = new ArrayList<String>();

	public static void main(String[] args) throws RemoteException,
			FileNotFoundException {

		PropertyConfigurator.configure(DEFAULT_GSN_LOG4J_PROPERTIES);
		String host = "http://micssrv22.epfl.ch/";
		VSensorConfig conf = new VSensorConfig();
		conf.setName("GSNTest");
		conf.setDescription("Desc1");
		conf.setAddressing(new KeyValue[] {
				new KeyValueImp("latitude", "46.4823313875"),
				new KeyValueImp("Longitude", "6.9873408131"),
				new KeyValueImp("Altitude", "2043.1780") });
		conf.setOutputStructure(new DataField[] {
				new DataField("rh", "integer"), new DataField("ths", "double"),
				new DataField("rh", "double") });
		String username = "gsn-user@gsn.com";
		String password = "NK3GHFYm";

		register_sensor(username, password, conf, host);
		// delete_sensor(username, password, conf);
	}

	/**
	 * Tries to register a virtual sensor. Returns an integer for status.
	 * 
	 * @param username
	 *            username for sensorweb user account
	 * @param password
	 *            password for sensorweb user account
	 * @param conf
	 *            configuration of the virtual sensor
	 * @param gsnURI
	 *            URI of the GSN instance
	 * @return status of operation is integer. 0 for not created, 1 for created
	 *         new, 2 for already exists.
	 * @throws RemoteException
	 * @throws java.rmi.RemoteException
	 *             java.rmi.RemoteException
	 */

	public static gsn.msr.sensormap.sensorman.ServiceStub.Guid login_to_sensor_map(
			String username, String password) throws RemoteException {
		logger.warn("Using username:" + username + " password:*****"); // mask
																		// password
		gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID;
		gsn.msr.sensormap.userman.ServiceStub login = new gsn.msr.sensormap.userman.ServiceStub();
		gsn.msr.sensormap.userman.ServiceStub.GetPassCode getPassCodeParams = new gsn.msr.sensormap.userman.ServiceStub.GetPassCode();
		getPassCodeParams.setUserName(username);
		getPassCodeParams.setPassword(password);
		String passcodeStr = login.GetPassCode(getPassCodeParams)
				.getGetPassCodeResult().getGuid();
		passGUID = new gsn.msr.sensormap.sensorman.ServiceStub.Guid();
		passGUID.setGuid(passcodeStr);
		logger.warn("Got GUID passcode:" + passcodeStr);
		return passGUID;
	}

	public static int register_sensor(String username, String password,
			VSensorConfig conf, String gsnURI) throws RemoteException,
			FileNotFoundException {

		gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID = login_to_sensor_map(
				username, password);
		HashMap<String, MetaData> output = MetaData.createMetaData(Main
				.getContainerConfig().getMsrMap().get("metadata"));

		logger
				.warn("Registering sensor " + conf.getName()
						+ " on Sensormap...");

		gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); // the
																										// default
																										// implementation
																										// should
																										// point
																										// to
																										// the
																										// right
																										// endpoint

		StringBuilder pComments = new StringBuilder();
		StringBuilder pNames = new StringBuilder();
		StringBuilder pMetadata = new StringBuilder();
		StringBuilder pTypes = new StringBuilder();
		String groupName = Main.getContainerConfig().getMsrMap().get(
				"group-name");

		for (DataField df : conf.getOutputStructure()) {
			MetaData metaData = output.get(df.getName().toLowerCase().trim());
			String pType = null;
			String pUnit = "";

			if (metaData != null) {
				pTypes.append(metaData.getSensorType()).append("|");
				pType = metaData.getSensorType();
				pMetadata.append(metaData.getMetadata()).append("|");
				pComments.append(metaData.getComments()).append("|");
				pNames.append(metaData.getSensorName()).append("|");
				pUnit = metaData.getUnit();
			} else {
				pTypes.append(df.getName()).append("|");
				pNames.append(df.getName()).append("|");
				pType = df.getName();
			}
			if (!dataTypeCache.contains(pType)) {
				gsn.msr.sensormap.sensorman.ServiceStub.CreateSingularSensorType createType = new gsn.msr.sensormap.sensorman.ServiceStub.CreateSingularSensorType();
				createType.setPublisherName(username);
				createType.setPassCode(passGUID);
				createType.setName(pType);
				createType.setUnit(pUnit);
				createType.setDataType("scalar");
				createType.setIconUrl("");
				String result = stub.CreateSingularSensorType(createType)
						.getCreateSingularSensorTypeResult();
				logger.info("Registering data type: " + pType
						+ " , MSR's output: " + result);
				dataTypeCache.add(pType);
			}

		}

		gsn.msr.sensormap.userman.ServiceStub.AddGroup createGroup = new gsn.msr.sensormap.userman.ServiceStub.AddGroup();
		gsn.msr.sensormap.userman.ServiceStub login = new gsn.msr.sensormap.userman.ServiceStub();
		createGroup.setGrpName(conf.getName());
		if (groupName != null && groupName.trim().length() > 0)
			createGroup.setParentGroup(groupName);
		createGroup.setAdmin(username);
		String call_output = login.AddGroup(createGroup).getAddGroupResult();
		logger.info("Creating a group called : " + conf.getName()
				+ " With Parent: " + groupName + " MSR: " + call_output);

		gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor deleteVSensorParam = new gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor();
		deleteVSensorParam.setOriginalPublisherName(username);
		deleteVSensorParam.setPublisherName(username);
		deleteVSensorParam.setPassCode(passGUID);
		deleteVSensorParam.setSensorName(conf.getName());
		deleteVSensorParam.setSensorType("GSN-" + conf.getName());

		call_output = stub.DeleteVectorSensor(deleteVSensorParam)
				.getDeleteVectorSensorResult();
		logger.info("Unregister: " + conf.getName() + " - MSR: " + call_output);

		gsn.msr.sensormap.sensorman.ServiceStub.RegisterCompositeSensor registerVectorSensorParams = new gsn.msr.sensormap.sensorman.ServiceStub.RegisterCompositeSensor();

		registerVectorSensorParams.setPublisherName(username);
		registerVectorSensorParams.setPassCode(passGUID);
		registerVectorSensorParams.setVectorSensorName(conf.getName());
		registerVectorSensorParams.setWsURL(gsnURI + "services/Service?wsdl");
		registerVectorSensorParams.setAlt(conf.getAltitude().toString());
		registerVectorSensorParams.setLat(conf.getLatitude().toString());
		registerVectorSensorParams.setLon(conf.getLongitude().toString());
		registerVectorSensorParams.setDesc(conf.getDescription());
		registerVectorSensorParams.setVectorSensorType("GSN-"+ conf.getName());
		registerVectorSensorParams.setParamTypes(pTypes.toString());
		registerVectorSensorParams.setParamComments(pComments.toString());
		registerVectorSensorParams.setParamMetaData(pMetadata.toString());
		registerVectorSensorParams.setParamNames(pNames.toString());
		registerVectorSensorParams.setAccessControl("protected");
		registerVectorSensorParams.setDataType("vector");
		registerVectorSensorParams.setIcon("image/CImg/weather_tower.gif");
		registerVectorSensorParams.setGroupName(conf.getName());

		call_output = stub.RegisterCompositeSensor(
				registerVectorSensorParams)
				.getRegisterCompositeSensorResult();

		if (call_output.indexOf("OK") >= 0) {
			logger.warn("Sensor " + conf.getName()
					+ " registered correctly. SensorMap says: " + call_output);
			return REGISTER_SENSOR_OK_CREATED_NEW;
		}

		if (call_output
				.indexOf("Error: Sensor with the same publisher name and sensor name already exists") >= 0) {
			logger.warn("Sensor " + conf.getName()
					+ " not registered (already exists). SensorMap says: "
					+ call_output);
			return REGISTER_SENSOR_ERROR_ALREADY_EXISTS;
		}

		logger.warn("Sensor " + conf.getName()
				+ " not registered. SensorMap says: " + call_output);
		return REGISTER_SENSOR_ERROR_NOT_CREATED;

	}

	/**
	 * Tries to delete a virtual sensor. Returns an integer for status.
	 * 
	 * @param username
	 *            username for sensorweb user account
	 * @param password
	 *            password for sensorweb user account
	 * @param conf
	 *            configuration of the virtual sensor
	 * @return status of operation is integer. 0 for not deleted (already
	 *         exists), 1 for deleted.
	 * @throws java.rmi.RemoteException
	 *             java.rmi.RemoteException
	 */
	public static int delete_sensor(String username, String password,
			VSensorConfig conf) throws RemoteException {

		logger.warn("Deleting sensor " + conf.getName() + " from Sensormap...");
		gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub();
		gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID = login_to_sensor_map(
				username, password);

		// create DeleteVectorSensor object
		gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor deleteVectorSensorParams = new gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor();
		deleteVectorSensorParams.setPassCode(passGUID);
		deleteVectorSensorParams.setPublisherName(username);
		deleteVectorSensorParams.setOriginalPublisherName(username);
		deleteVectorSensorParams.setSensorName(conf.getName());

		logger.warn("Calling DeleteVectorSensor with parameters:");
		logger.warn("...passCode: " + deleteVectorSensorParams.getPassCode());
		logger.warn("...PublisherName: "
				+ deleteVectorSensorParams.getPublisherName());
		logger.warn("...OriginalPublisherName: "
				+ deleteVectorSensorParams.getOriginalPublisherName());
		logger.warn("...SensorName: "
				+ deleteVectorSensorParams.getSensorName());

		gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensorResponse output = stub
				.DeleteVectorSensor(deleteVectorSensorParams);
		String call_output = output.getDeleteVectorSensorResult();

		if (call_output.indexOf("OK") >= 0) {
			logger.warn("Sensor " + conf.getName()
					+ " deleted correctly. SensorMap says: " + call_output);
			return DELETE_SENSOR_OK;
		} else {
			logger.warn("Sensor " + conf.getName()
					+ " not deleted. SensorMap says: " + call_output);
			return DELETE_SENSOR_ERROR_DOESNT_EXIST;
		}
	}

}
