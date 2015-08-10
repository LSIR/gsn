package tinygsn.model.vsensor;

import java.io.Serializable;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;

public class METVirtualSensor extends AbstractVirtualSensor {
	
	
	private static final long serialVersionUID = -5276247717926554522L;
	double weight = 60.0;
	double age = 30;
	String gender = "m";
	private DataField[] outputStructure = new DataField[]{new DataField("MET",DataTypes.DOUBLE),new DataField("VA",DataTypes.DOUBLE)};
	double[] MET_Table = new double[]{3,1.8,10,7.5,5,1.3};
	StreamElement lastActivity = null;
	//3.bike : 7.5
	//5.sitting : 1.3
	//1.standing : 1.8
	//2.running : 10
	//4.stairs : 5
	//0.walking : 3
	
	private double getRMR(){
		if (gender.equals("m")){
			if (age<30){
				return 2.896 + weight * 0.063;
			}else if (age<60){
				return 3.653 + weight * 0.048;
			}else{
				return 2.459 + weight * 0.049;
			}
		}else{
			if (age<30){
				return 2.036 + weight * 0.062;
			}else if (age<60){
				return 3.538 + weight * 0.034;
			}else{
				return 2.755 + weight * 0.038;
			}
		}
	}
	
	private double getNVO2max(){
		double n = 0;
		
		if (gender.equals("m")){
			n = 44 / 2.0;
		}else{
			n = 33.8 / 2.0;
		}
		
		if (age<30){
			n += 45.3 / 2.0;
		}else if(age<40){
			n += 43.8 / 2.0;
		}else if(age<50){
			n += 42.9 / 2.0;
		}else if(age<60){
			n += 36.8 / 2.0;
		}else if(age<70){
			n += 30.7 / 2.0;
		}else{
			n += 27.2 / 2.0;
		}
		
		return n;
	}

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {}

	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		if(lastActivity == null){
			lastActivity = streamElement;
		}
		if(lastActivity.getData("activity") != streamElement.getData("activity")){
			double MET = MET_Table[((Double)lastActivity.getData("activity")).intValue()];
			double ECF = 0.21;
			double RMR = getRMR();
			double NVO2max = getNVO2max();
			double VA = 19.63 * Math.min(ECF*MET*RMR, NVO2max*weight*(1.212-0.14*Math.log((streamElement.getTimeStamp()-lastActivity.getTimeStamp())/60000.0)));
			dataProduced(new StreamElement(outputStructure, new Serializable[]{MET,VA}, lastActivity.getTimeStamp()));
			lastActivity = streamElement;
		}
		

		
	}

	@Override
	public String[] getParameters(){
		return new String[]{"weight","age","gender(m/f)"};
		}
	
	@Override
	protected void initParameter(String key, String value){
		if (key.endsWith("weight")){
			weight = Double.parseDouble(value);
		}else if(key.endsWith("age")){
			age = Integer.parseInt(value);
		}else if(key.endsWith("gender(m/f)")){
			gender = value;
		}
	}
	
	@Override
	public DataField[] getOutputStructure(DataField[] in){
		return outputStructure;
	}
}
