package tinygsn.model.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;
import android.util.Log;

public class AndroidFakeTemperatureWrapper extends AbstractWrapper {

	private static final int DEFAULT_SAMPLING_RATE = 500;

	private static final String[] FIELD_NAMES = new String[] { "temp" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] { "Temp Reading" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double" };

	private int samplingRate = DEFAULT_SAMPLING_RATE;

	private final String TAG = this.getClass().getSimpleName();

	private static int threadCounter = 0;

	public AndroidFakeTemperatureWrapper() {
		super();
	}

	public AndroidFakeTemperatureWrapper(Queue queue) {
		super(queue);
	}

	public boolean initialize() {
		return true;
	}

	public void run() {
		Log.v(TAG, " waiting for data");
		double temp = 23;

		while (isActive()) {
			temp = tinygsn.utils.MathUtils.getNextRandomValue(temp, -10, 35, 1);
			try {
				Thread.sleep(samplingRate);
			}
			catch (InterruptedException e) {
				Log.e(e.getMessage(), e.toString());
			}
			Date time = new Date();
			StreamElement streamElement = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					new Serializable[] { temp }, time.getTime());
			postStreamElement(streamElement);
		}
	}

	public void dispose() {
		threadCounter--;
	}

	public String getWrapperName() {
		return TAG;
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