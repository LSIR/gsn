package gsn.vsensor;

import gsn.beans.StreamElement;

public class OpensenseDisalConverter extends AbstractVirtualSensor {

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {

	}
	
	private double convertValue(String val){
		return Integer.parseInt(val.substring(0, 2)) + Double.parseDouble(val.substring(2)) / 60.0;
	}

	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		for(int i = 0; i<streamElement.getData().length; i++ ){
			if (streamElement.getFieldNames()[i].equalsIgnoreCase("latitude")){
				streamElement.setData(i, convertValue(streamElement.getData()[i].toString()));
			}
		}
		
		dataProduced(streamElement);

	}

}
