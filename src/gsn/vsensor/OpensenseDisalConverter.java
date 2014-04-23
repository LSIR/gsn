package gsn.vsensor;

import gsn.beans.StreamElement;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class OpensenseDisalConverter extends AbstractVirtualSensor {
	
	Pattern pattern = Pattern.compile("(\\d+)(\\d{2}\\.\\d+)");

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {

	}
	/**
	 * Convert the longitude and latitude fields expressed in NMEA format (ex.: 4633.43993 stands for 46 degrees 33.43993 minutes)
	 * @param val
	 * @return
	 */
	
	private double convertValue(String val){
		System.out.println();
		Matcher matcher = pattern.matcher(val);
		if(matcher.find()){
			return Integer.parseInt(matcher.group(1)) + Double.parseDouble(matcher.group(2)) / 60.0;
		}else{
			return 0.0;
		}
	}

	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		for(int i = 0; i<streamElement.getData().length; i++ ){
			if (streamElement.getFieldNames()[i].equalsIgnoreCase("latitude") || streamElement.getFieldNames()[i].equalsIgnoreCase("longitude")){
				streamElement.setData(i, convertValue(streamElement.getData()[i].toString()));
			}
		}
		
		dataProduced(streamElement);

	}

}
