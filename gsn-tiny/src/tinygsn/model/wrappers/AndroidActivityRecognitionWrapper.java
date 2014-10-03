package tinygsn.model.wrappers;

import java.util.ArrayList;

import android.util.Log;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.Queue;
import tinygsn.beans.StreamElement;

public class AndroidActivityRecognitionWrapper extends AbstractWrapper {

	private static final String[] FIELD_NAMES = new String[] { "type", "confidence" };

	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE,
			DataTypes.DOUBLE };

	private static final String[] FIELD_DESCRIPTION = new String[] { "type", "confidenct" };

	private static final String[] FIELD_TYPES_STRING = new String[] { "double", "double" };

	private static final String TAG = "AndroidActivityRecognitionWrapper";

	private StreamElement theLastStreamElement = null;
	
	public AndroidActivityRecognitionWrapper()
	{
		super();
	}
	
	public AndroidActivityRecognitionWrapper(Queue queue) {
		super(queue);
		initialize();
	}

	public boolean initialize() {
		return true;
	}

	
	public void getLastKnownData() {
		if (getTheLastStreamElement() == null) {
			Log.e(TAG, "There is no signal!");
		}
		else {
			postStreamElement(getTheLastStreamElement());
		}
	}


	public String getWrapperName() {
		return this.getClass().getSimpleName();
	}
	
	

	public StreamElement getTheLastStreamElement() {
		return theLastStreamElement;
	}

	public void setTheLastStreamElement(StreamElement theLastStreamElement) {
		this.theLastStreamElement = theLastStreamElement;
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
