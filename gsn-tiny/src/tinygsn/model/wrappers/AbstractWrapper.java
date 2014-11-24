/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/wrappers/AbstractWrapper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import tinygsn.beans.AddressBean;
import tinygsn.beans.DataField;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import android.content.Context;
import android.util.Log;

public abstract class AbstractWrapper extends Thread {

	private static final String TAG = "AbstractWrapper";

//	public static final String[] WRAPPER_LIST = { "accelerometer", "light",
//			"orientation", "magnetic", "proximity", "gps", "fakeGPS",
//			"fakeTemperature" };
//	public static final String[] WRAPPER_CLASS = {
//			"tinygsn.model.wrappers.AndroidAccelerometerWrapper",
//			"tinygsn.model.wrappers.AndroidLightWrapper",
//			"tinygsn.model.wrappers.AndroidOrientationWrapper",
//			"tinygsn.model.wrappers.AndroidMagneticFieldWrapper",
//			"tinygsn.model.wrappers.AndroidProximityWrapper",
//			"tinygsn.model.wrappers.AndroidGPSWrapper",
//			"tinygsn.model.wrappers.AndroidFakeGPSWrapper",
//			"tinygsn.model.wrappers.AndroidFakeTemperatureWrapper" };

	protected List<Properties> predicates = new Vector<Properties>();

	protected static final int DEFAULT_SAMPLING_RATE = 500;
	protected int samplingRate = DEFAULT_SAMPLING_RATE;

	protected final List<StreamSource> listeners = Collections
			.synchronizedList(new ArrayList<StreamSource>());

	public boolean hasPredicates() {
		return !predicates.isEmpty();
	}

	private VSensorConfig config = null;

	private AddressBean activeAddressBean;

	private boolean isActive = true;

	public static final int GARBAGE_COLLECT_AFTER_SPECIFIED_NO_OF_ELEMENTS = 2;

	private Queue queue = null;

	public AbstractWrapper() {
	}

	public AbstractWrapper(Queue queue) {
		this.queue = queue;
	}

	public Queue getQueue() {
		return queue;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public int getSamplingRate() {
		return samplingRate;
	}

	public void setSamplingRate(int samplingRate) {
		this.samplingRate = samplingRate;
	}

	/**
	 * The output structure should be specified in the XML config file. However,
	 * for the simplicity of this tinygsn version, we return it from wrapper.
	 * 
	 * @return
	 */
	public abstract DataField[] getOutputStructure();

	/**
	 * Returns the view name created for this listener. Note that, GSN creates one
	 * view per listener.
	 * 
	 * @throws SQLException
	 */
	public void addListener(StreamSource ss) throws SQLException {
	}

	public boolean isActive() {
		return isActive;
	}

	/**
	 * This method gets the generated stream element and notifies the input
	 * streams if needed. The return value specifies if the newly provided stream
	 * element generated at least one input stream notification or not.
	 * 
	 * @param streamElement
	 * @return If the method returns false, it means the insertion doesn't
	 *         effected any input stream.
	 */

	protected Boolean postStreamElement(StreamElement streamElement) {
		queue.add(streamElement);
		return true;
	}

	/**
	 * Updates the table representing the data items produced by the stream
	 * element. Returns false if the update fails or doesn't change the state of
	 * the table.
	 * 
	 * @param se
	 *          Stream element to be inserted to the table if needed.
	 * @return true if the stream element is successfully inserted into the table.
	 * @throws SQLException
	 */
	public boolean insertIntoWrapperTable(StreamElement se) throws SQLException {
		return true;
	}

	public void releaseResources() {
		Log.i("deactivation", this.toString());
		isActive = false;
	}

	public abstract String[] getFieldList();

	public abstract Byte[] getFieldType();

	public VSensorConfig getConfig() {
		return config;
	}

	public void setConfig(VSensorConfig config) {
		this.config = config;
	}

	public static Properties getWrapperList(Context context) {
		Properties wrapperList = new Properties();
		try {
			InputStream is = context.getAssets().open("wrapper_list.properties");
			wrapperList.load(is);
			//TODO Check if the sensor is available on this phone
		}
		catch (IOException e) {
			Log.e(TAG, e.toString());
		}

		return wrapperList;
	}
}
