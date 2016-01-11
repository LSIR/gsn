package tinygsn.model.vsensor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.epfl.locationprivacy.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.model.utils.ParameterType;
import tinygsn.model.utils.Parameter;

import static android.os.Debug.startMethodTracing;
import static android.os.Debug.stopMethodTracing;

public class CalibrateOzoneVirtualSensor extends AbstractVirtualSensor {

	/**
	 *
	 */
	private static final long serialVersionUID = -6312509806997497585L;
	private String modelUrl;
	private String modelName;
	//source: 0=local sensor, 1=remote model, 2=local calibrated
	private DataField[] outputStructure = new DataField[]{new DataField("o3", DataTypes.DOUBLE), new DataField("source", DataTypes.INTEGER)};
	private double lastLatitude;
	private double lastLongitude;
	private long lastLocationTime = 0;
	private SimpleRegression sr = new SimpleRegression();

	private String LOGTAG = "CalibrateOzoneVirtualSensor";


	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		double ozone = 0;
		double model = 0;
		int source = 0;

		if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE")) {
			startMethodTracing("Android/data/tinygsn.gui.android/" + LOGTAG + "_" + inputStreamName + "_" + System.currentTimeMillis());
		}

		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "===========================================");
		log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Starting to process data in dataAvailable");
		long startLogTime = System.currentTimeMillis();

		streamElement = super.anonymizeData(inputStreamName, streamElement);

		if (inputStreamName.endsWith("gps")) {
			lastLatitude = (Double) streamElement.getData("latitude");
			lastLongitude = (Double) streamElement.getData("longitude");
			lastLocationTime = streamElement.getTimeStamp();
		} else {
			double measure = (Double) streamElement.getData("ozoneCalibrated");
			if (System.currentTimeMillis() - lastLocationTime < 60000) {
				model = getModelValue();
				if (model > -1) {
					sr.addData(measure, model);
				}
			}
			if (System.currentTimeMillis() - lastLocationTime < 15000) {
				ozone = model;
				source = 1;
			} else {
				double ozone_p = sr.predict(measure);
				if (!Double.isNaN(ozone_p)) {
					ozone = ozone_p;
					source = 2;
				} else {
					ozone = measure;
				}
			}

			long endLogTime = System.currentTimeMillis();
			log("dataAvailable_" + LOGTAG + "_" + inputStreamName, "Total Time to process data in dataAvailable() (without dataProduced()) : " + (endLogTime - startLogTime) + " ms.");

			dataProduced(new StreamElement(outputStructure, new Serializable[]{ozone, source}, streamElement.getTimeStamp()));

			if ((boolean) Utils.getBuildConfigValue(StaticData.globalContext, "PERFORMANCE") || (boolean) Utils.getBuildConfigValue(StaticData.globalContext, "GPSPERFORMANCE")) {
				stopMethodTracing();
			}
		}
	}

	private double getModelValue() {
		try {
			double ozone = -1;
			HttpGet httpGet;
			DefaultHttpClient httpclient = new DefaultHttpClient();
			httpGet = new HttpGet("http://" + modelUrl + "/modeldata?vs=" + modelName + "&models=0&format=json&latitude=" + lastLatitude + "&longitude=" + lastLongitude);
			HttpResponse response = httpclient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			InputStreamReader is = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
			if (statusCode == 200) {
				BufferedReader bufferedReader = new BufferedReader(is);
				String line = bufferedReader.readLine();
				if (line != null) {
					JSONArray obj = new JSONArray(line);
					JSONArray f = obj.getJSONObject(0).getJSONArray("fields");
					for (int i = 1; i < f.length(); i++) {
						JSONObject v = f.getJSONObject(i);
						if (v.getString("name").equalsIgnoreCase("O3_RES")) {
							ozone = v.getDouble("value");
							break;
						}
					}
				}
			}
			is.close();
			return ozone;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public ArrayList<Parameter> getParameters() {
		ArrayList<Parameter> list = new ArrayList<>();
		list.add(new Parameter("server_url", ParameterType.EDITBOX));
		list.add(new Parameter("model_name", ParameterType.EDITBOX));
		return list;
	}

	@Override
	protected void initParameter(String key, String value) {
		if (key.endsWith("server_url")) {
			modelUrl = value;
		} else if (key.endsWith("model_name")) {
			modelName = value;
		}
	}

	@Override
	public DataField[] getOutputStructure(DataField[] in) {
		return outputStructure;
	}


}
