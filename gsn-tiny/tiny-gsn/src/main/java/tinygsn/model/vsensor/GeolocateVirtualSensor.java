package tinygsn.model.vsensor;

import java.io.Serializable;
import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;

import tinygsn.beans.StreamElement;
import tinygsn.model.utils.Parameter;


public class GeolocateVirtualSensor extends AbstractVirtualSensor {


	private DataField[] outputStructure = null;
	private Double lastLatitudeTL = 0.0;
	private Double lastLongitudeTL = 0.0;
    private Double lastLatitudeBR = 0.0;
    private Double lastLongitudeBR = 0.0;
	private Long lastLocationTime = 0L;

	private String LOGTAG = "GeolocateVirtualSensor";


	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {

		streamElement = super.anonymizeData(inputStreamName, streamElement);

		if (inputStreamName.endsWith("AndroidGPSWrapper")) {
			lastLatitudeTL = (Double) streamElement.getData("latitudeTopLeft");
			lastLongitudeTL = (Double) streamElement.getData("longitudeTopLeft");
            lastLatitudeBR = (Double) streamElement.getData("latitudeBottomRight");
            lastLongitudeBR = (Double) streamElement.getData("longitudeBottomRight");
			lastLocationTime = streamElement.getTimeStamp();
		} else {
            Serializable[] data = new Serializable[outputStructure.length];
            for (int i=0;i< data.length-5;i++){
                data[i] = streamElement.getData()[i];
            }
            data[data.length-5] = lastLatitudeTL;
            data[data.length-4] = lastLongitudeTL;
            data[data.length-3] = lastLatitudeBR;
            data[data.length-2] = lastLongitudeBR;
            data[data.length-1] = lastLocationTime;

			dataProduced(new StreamElement(outputStructure, data, streamElement.getTimeStamp()));
		}
	}

	@Override
	public ArrayList<Parameter> getParameters() {
		ArrayList<Parameter> list = new ArrayList<>();
		return list;
	}

	@Override
	protected void initParameter(String key, String value) {
	}

	@Override
	public DataField[] getOutputStructure(DataField[] in) {

		for (DataField df : in){
			if (df.getName().startsWith("latitude") || df.getName().startsWith("longitude")){
				return outputStructure;
			}
		}
		outputStructure = new DataField[in.length + 5];
        for(int i=0;i<in.length;i++){
            outputStructure[i] = in[i];
        }
        outputStructure[in.length] = new DataField("latitudeTopLeft", DataTypes.DOUBLE);
        outputStructure[in.length+1] = new DataField("longitudeTopLeft", DataTypes.DOUBLE);
        outputStructure[in.length+2] = new DataField("latitudeBottomRight", DataTypes.DOUBLE);
        outputStructure[in.length+3] = new DataField("longitudeBottomRight", DataTypes.DOUBLE);
        outputStructure[in.length+4] = new DataField("locationTime", DataTypes.BIGINT);

        return outputStructure;
	}


}
