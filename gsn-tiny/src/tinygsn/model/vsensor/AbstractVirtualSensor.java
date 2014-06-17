package tinygsn.model.vsensor;

import java.io.Serializable;
import java.sql.SQLException;
import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.controller.AbstractController;
import android.util.Log;

public abstract class AbstractVirtualSensor {

	public static String[] VIRTUAL_SENSOR_LIST = { "bridge", "notification" };
	private static final String TAG = "AbstractVirtualSensor";

	public static final String PROCESSING_CLASS_BRIDGE = "tinygsn.model.vsensor.BridgeVirtualSensor";
	public static final String PROCESSING_CLASS_NOTIFICATION = "tinygsn.model.vsensor.NotificationVirtualSensor";

	private VSensorConfig config;

	public abstract boolean initialize();

	// synchronized

	protected void dataProduced(StreamElement streamElement, boolean adjust) {

		// Send data to controller -> update view
		config.getController().consume(streamElement);
		AbstractController controller = config.getController();

		try {
			controller.getStorageManager().executeInsert("vs_" + config.getName(),
					null, streamElement);
			Log.v(TAG,
					"Inserted: " + streamElement.toString() + " to " + config.getName());
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calls the dataProduced with adjust = false.
	 * 
	 * @param streamElement
	 */
	protected synchronized void dataProduced(StreamElement streamElement) {
		dataProduced(streamElement, true);
	}

	/**
	 * First checks compatibility of the data type of each output data item in the
	 * stream element with the defined output in the VSD file. (this check is done
	 * regardless of the value for adjust flag).
	 * <p>
	 * If the adjust flag is set to true, the method checks the newly generated
	 * stream element and returns true if and only if the number of data items is
	 * equal to the number of output data structure defined for this virtual
	 * sensor. If the adjust=true, then this test is not performed.
	 * 
	 * @param se
	 * @param outputStructure
	 * @param adjust
	 *          default is false.
	 * @return
	 */
	private static boolean compatibleStructure(StreamElement se,
			DataField[] outputStructure, boolean adjust) {
		// if (!adjust && outputStructure.length != se.getFieldNames().length ) {
		// logger.warn(
		// "Validation problem, the number of field doesn't match the number of output data strcture of the virtual sensor"
		// );
		// return false;
		// }
		// int i =-1;
		// for (DataField field: outputStructure) {
		// Serializable value = se.getData(field.getName());
		// i++;
		// if (value==null)
		// continue;
		// if ( ( ( field.getDataTypeID() == DataTypes.BIGINT ||
		// field.getDataTypeID() == DataTypes.DOUBLE ||
		// field.getDataTypeID() == DataTypes.INTEGER||
		// field.getDataTypeID() == DataTypes.SMALLINT||
		// field.getDataTypeID() == DataTypes.TINYINT ) &&!(value instanceof
		// Number))
		// ||
		// ( (field.getDataTypeID() == DataTypes.VARCHAR || field.getDataTypeID() ==
		// DataTypes.CHAR) && !(value instanceof String)) ||
		// ( (field.getDataTypeID() == DataTypes.BINARY) && !(value instanceof
		// byte[]))
		// ){
		// logger.warn( "Validation problem for output field >" + field.getName( ) +
		// ", The field type declared as >" +
		// field.getType()+"< while in VSD it is defined as >"+DataTypes.TYPE_NAMES[outputStructure[
		// i ].getDataTypeID( )]);
		// return false;
		// }
		// }
		return true;
	}

	/**
	 * Called when the container want to stop the pool and remove it's resources.
	 * The container will call this method once on each install of the virtual
	 * sensor in the pool. The progrmmer should release all the resouce used by
	 * this virtual sensor instance in this method specially those resouces
	 * aquired during the <code>initialize</code> call.
	 * <p/>
	 * Called once while finalizing an instance of the virtual sensor
	 */
	public abstract void dispose();

	public boolean dataFromWeb(String action, String[] paramNames,
			Serializable[] paramValues) {
		return false;
	}

	/**
	 * @return the virtualSensorConfiguration
	 */
	public VSensorConfig getVirtualSensorConfiguration() {
		if (config == null) {
			throw new RuntimeException("The VirtualSensorParameter is not set !!!");
		}
		return config;
	}

	/**
	 * @param virtualSensorConfiguration
	 *          the virtualSensorConfiguration to set
	 */
	public void setVirtualSensorConfiguration(
			VSensorConfig virtualSensorConfiguration) {
		this.config = virtualSensorConfiguration;
	}

	/**
	 * This method is going to be called by the container when one of the input
	 * streams has a data to be delivered to this virtual sensor. After receiving
	 * the data, the virutal sensor can do the processing on it and this
	 * processing could possibly result in producing a new stream element in this
	 * virtual sensor in which case the virutal sensor will notify the container
	 * by simply adding itself to the list of the virtual sensors which have
	 * produced data. (calling <code>container.publishData(this)</code>. For more
	 * information please check the <code>AbstractVirtalSensor</code>
	 * 
	 * @param inputStreamName
	 *          is the name of the input stream as specified in the configuration
	 *          file of the virtual sensor.
	 * @param streamElement
	 *          is actually the real data which is produced by the input stream
	 *          and should be delivered to the virtual sensor for possible
	 *          processing.
	 */
	public abstract void dataAvailable(String inputStreamName,
			StreamElement streamElement);

}
