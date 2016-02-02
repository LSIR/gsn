package tinygsn.model.vsensor;

import org.epfl.locationprivacy.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.utils.ParameterType;
import tinygsn.model.utils.Parameter;

import static android.os.Debug.startMethodTracing;
import static android.os.Debug.stopMethodTracing;

public class METVirtualSensor extends AbstractVirtualSensor {


	private static final long serialVersionUID = -5276247717926554522L;
	double weight = 60.0;
	double age = 30;
	String gender = "m";
	private DataField[] outputStructure = new DataField[]{new DataField("MET", DataTypes.DOUBLE), new DataField("VA", DataTypes.DOUBLE), new DataField("start", DataTypes.BIGINT), new DataField("end", DataTypes.BIGINT)};
	double[] MET_Table = new double[]{3, 1.8, 10, 7.5, 5, 1.3};
	StreamElement lastActivity = null;

	private String LOGTAG = "METVirtualSensor";
	//3.bike : 7.5
	//5.sitting : 1.3
	//1.standing : 1.8
	//2.running : 10
	//4.stairs : 5
	//0.walking : 3

	private double getRMR() {
		if (gender.equals("m")) {
			if (age < 30) {
				return 2.896 + weight * 0.063;
			} else if (age < 60) {
				return 3.653 + weight * 0.048;
			} else {
				return 2.459 + weight * 0.049;
			}
		} else {
			if (age < 30) {
				return 2.036 + weight * 0.062;
			} else if (age < 60) {
				return 3.538 + weight * 0.034;
			} else {
				return 2.755 + weight * 0.038;
			}
		}
	}

	private double getNVO2max() {
		double n = 0;

		if (gender.equals("m")) {
			n = 44 / 2.0;
		} else {
			n = 33.8 / 2.0;
		}

		if (age < 30) {
			n += 45.3 / 2.0;
		} else if (age < 40) {
			n += 43.8 / 2.0;
		} else if (age < 50) {
			n += 42.9 / 2.0;
		} else if (age < 60) {
			n += 36.8 / 2.0;
		} else if (age < 70) {
			n += 30.7 / 2.0;
		} else {
			n += 27.2 / 2.0;
		}

		return n;
	}

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {

		if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE")) {
			startMethodTracing("Android/data/tinygsn.gui.android/" + LOGTAG + "_" + inputStreamName + "_" + System.currentTimeMillis());
		}
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "===========================================");
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Starting to process data in dataAvailable");
		long startLogTime = System.currentTimeMillis();

		streamElement = super.anonymizeData(inputStreamName, streamElement);

		if (lastActivity == null) {
			lastActivity = streamElement;
		}
		if (lastActivity.getData("activity") != streamElement.getData("activity")) {
			double MET = MET_Table[((Double) lastActivity.getData("activity")).intValue()];
			double ECF = 0.21;
			double RMR = getRMR();
			double NVO2max = getNVO2max();
			double VAmax = NVO2max * weight * (1.212 - 0.14 * Math.log((streamElement.getTimeStamp() - lastActivity.getTimeStamp()) / 60000.0));
			double VA = 19.63 * ECF * MET * RMR;
			if (VAmax != Double.NaN) {
				VA = 19.63 * Math.min(ECF * MET * RMR, VAmax);
			}

			long endLogTime = System.currentTimeMillis();
			log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Total Time to process data in dataAvailable() (without dataProduced()) : " + (endLogTime - startLogTime) + " ms.");

			dataProduced(new StreamElement(outputStructure, new Serializable[]{MET, VA, lastActivity.getTimeStamp(), streamElement.getTimeStamp()}, streamElement.getTimeStamp()));
			lastActivity = streamElement;
			if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE") || (boolean) Utils.getBuildConfigValue(StaticData.globalContext, "GPSPERFORMANCE")) {
				stopMethodTracing();
			}
		}


	}

	@Override
	public ArrayList<Parameter> getParameters() {
		ArrayList<Parameter> list = new ArrayList<>();
		list.add(new Parameter("weight", ParameterType.EDITBOX));
		list.add(new Parameter("age", ParameterType.EDITBOX));
		list.add(new Parameter("gender(m/f)", ParameterType.EDITBOX));
		return list;
	}

	@Override
	protected void initParameter(String key, String value) {
		if (key.endsWith("weight")) {
			weight = Double.parseDouble(value);
		} else if (key.endsWith("age")) {
			age = Integer.parseInt(value);
		} else if (key.endsWith("gender(m/f)")) {
			gender = value;
		}
	}

	@Override
	public DataField[] getOutputStructure(DataField[] in) {
		return outputStructure;
	}
}
