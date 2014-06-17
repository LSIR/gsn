package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;
import android.util.Log;

public class AndroidFakeGPSWrapper extends AbstractWrapper {

	private static final String[] FIELD_NAMES = new String[] { "latitude",
			"longitude" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] {
			"Latitude Reading", "Longitude Reading" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double",
			"double" };

	private static final String TAG = "AndroidGPSWrapper";

	private static int threadCounter = 0;

	public AndroidFakeGPSWrapper() {
		super();
	}

	public AndroidFakeGPSWrapper(Queue queue) {
		super(queue);
	}

	public boolean initialize() {
		return false;

	}

	public void run() {
		Log.v(TAG, " waiting for data");

		double latitude = 37.4419;
		double longitude = -12.1419;
		
		while (isActive()) {
			latitude = tinygsn.utils.MathUtils.getNextRandomValue(latitude, -100, 100, 0.3);
			longitude = tinygsn.utils.MathUtils.getNextRandomValue(longitude, -100, 100, 0.3);
			
			try {
				Thread.sleep(samplingRate);
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
			Date time = new Date();
			StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					new Serializable[] { latitude, longitude }, time.getTime());
			postStreamElement(streamElement);
		}
	}

	public void dispose() {
		threadCounter--;
	}

	public String getWrapperName() {
		return "AndroidGPSWrapper";
	}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[] {});
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}

}