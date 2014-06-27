package gsn.vsensor;


import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.BinaryParser;


public class OpensenseSplitterVSensor extends AbstractVirtualSensor {
	

	private final transient Logger logger = Logger.getLogger( OpensenseSplitterVSensor.class );
	
	private final static String PARAM_DATA_TYPE = "type";
	private static final HashMap<String,DataField[]> dataFields = new HashMap<String,DataField[]>();    
	static{
		dataFields.put("FPM",new DataField[]{
			new DataField("station","smallint"),
	        new DataField("LDSA","double"),
	        new DataField("idiff","double"),
	        new DataField("HV","smallint"),
	        new DataField("EM","double")});
		dataFields.put("FPH", new DataField[]{   
		    new DataField("station","smallint"),
	        new DataField("ufp_temp","double"),
	        new DataField("ufp_RH","tinyint"),
	        new DataField("ufp_bat","double"),
	        new DataField("ufp_flow","double"),
	        new DataField("ufp_warning","tinyint")});
	    dataFields.put("OZONE", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("O3_res","int"),
	        new DataField("temp","double"),
	        new DataField("RH","tinyint")});
	    dataFields.put("ACC", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("accel_x","double"),
	        new DataField("accel_y","double"),
	        new DataField("accel_z","double")});
	    dataFields.put("CO", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("CO","int"),
	        new DataField("NO2","int"),
	        new DataField("CO2","int")});
	    dataFields.put("GPS", new DataField[]{
			new DataField("station","smallint"),
			new DataField("latitude","double"),
			new DataField("longitude","double"),
            new DataField("altitude","double"),
	        new DataField("azimuth_g","double"),
	        new DataField("speed","double"),
	        new DataField("satellites","TINYINT"),
	        new DataField("HDOP","double"),
	        new DataField("gyro","double"),
	        new DataField("ubx_temp","double")});
	    dataFields.put("TL", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("door_state","tinyint"),
	        new DataField("tl_line","tinyint"),
	        new DataField("tl_destination","varchar(20)"),
	        new DataField("tl_curr_stop","varchar(20)"),
	        new DataField("tl_next_stop","varchar(20)")});
	    dataFields.put("ODO", new DataField[]{
	    	new DataField("station","smallint"),
	    	new DataField("odometer","int")});
	    dataFields.put("MICSCO", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("CO_mics","int"),
	        new DataField("NO2_mics","int"),
	        new DataField("temp_mics","int"),
	        new DataField("RH_mics","smallint")});
	    
	}
	
	private DataField[] structure;
	private String data_type;
	private StreamElement temp;
	private HashMap<Short,StreamElement> last_values;

	


	@Override
	public boolean initialize() {
		TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();
        data_type = params.get(PARAM_DATA_TYPE);

        if (data_type == null) {
            logger.warn("Parameter \"" + PARAM_DATA_TYPE + "\" not provided in Virtual Sensor file");
            return false;
        }
        if(dataFields.containsKey(data_type.toUpperCase())){
        	structure = dataFields.get(data_type.toUpperCase());
        }else{
            logger.warn("Parameter \"" + PARAM_DATA_TYPE + "\" has an invalid value.");
            return false;
        }
        temp = new StreamElement(structure, new Serializable[structure.length]);
        if(data_type.equalsIgnoreCase("TL")){
        	last_values = new HashMap<Short,StreamElement>();
        }
		return true;
	}

	@Override
	public void dispose() {
	
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		try{
			short s_type = (Short) streamElement.getData("type");
			short id = (Short) streamElement.getData("station");
			Long time = (Long) streamElement.getData("timestamp"); 
			temp.setData(0,id);
			temp.setTimeStamp(time);
			ByteArrayInputStream input = new ByteArrayInputStream((byte[]) streamElement.getData("payload"));
			if(data_type.equalsIgnoreCase("FPH")){
				 if(s_type == 12){//sSCCS
					 temp.setData(1,BinaryParser.readNextShort(input, true)/10.0);
					 temp.setData(2,BinaryParser.readNextShort(input, false));
					 temp.setData(4,BinaryParser.readNextChar(input, false) / 100.0);
					 temp.setData(5,BinaryParser.readNextChar(input, false));
					 temp.setData(3,BinaryParser.readNextShort(input, false) / 100.0);
					 dataProduced(new StreamElement(temp));
				 } 
			} else if (data_type.equalsIgnoreCase("FPM")){
				 if(s_type == 13){ //LCSs
					 temp.setData(1,BinaryParser.readNextLong(input, false) / 100.0);
					 temp.setData(2,BinaryParser.readNextChar(input, false) / 10.0);
					 temp.setData(3,BinaryParser.readNextShort(input, false));
					 temp.setData(4,BinaryParser.readNextShort(input, true) / 100.0);
					 dataProduced(new StreamElement(temp));
				 }
	        } else if (data_type.equalsIgnoreCase("OZONE")){
	        	if(s_type == 10 || s_type == 30){//CSSC
	        		BinaryParser.readNextChar(input, false);
	        		temp.setData(1,BinaryParser.readNextShort(input, false));
	        		temp.setData(2,BinaryParser.readNextShort(input, false)/10.0 - 40);
	        		temp.setData(3,BinaryParser.readNextChar(input, false));
	        		dataProduced(new StreamElement(temp));
				 }       	
	        } else if (data_type.equalsIgnoreCase("ACC")){
	        	if(s_type == 6){//sss
	        		temp.setData(1,BinaryParser.readNextShort(input, true)/1000.0);
	        		temp.setData(2,BinaryParser.readNextShort(input, true)/1000.0);
	        		temp.setData(3,BinaryParser.readNextShort(input, true)/-1000.0);
	        		dataProduced(new StreamElement(temp));
				 }
	        } else if (data_type.equalsIgnoreCase("CO")){
	        	if(s_type == 9 || s_type == 29){ //SSS
	        		temp.setData(1,BinaryParser.readNextShort(input, false));
	        		temp.setData(2,BinaryParser.readNextShort(input, false));
	        		temp.setData(3,BinaryParser.readNextShort(input, false));
	        		dataProduced(new StreamElement(temp)); 
				 }
	        } else if (data_type.equalsIgnoreCase("ODO")){
	        	if(s_type == 1){ //S
	        		temp.setData(1,BinaryParser.readNextShort(input, false));
	        		dataProduced(new StreamElement(temp)); 
				 }
	        } else if (data_type.equalsIgnoreCase("MICSCO")){
	        	if(s_type == 11 || s_type == 31){ //SSSC
	        		temp.setData(1,BinaryParser.readNextShort(input, false));
	        		temp.setData(2,BinaryParser.readNextShort(input, false));
	        		temp.setData(3,BinaryParser.readNextShort(input, false));
	        		temp.setData(4,BinaryParser.readNextChar(input, false));
	        		dataProduced(new StreamElement(temp)); 
				 }
	        } else if (data_type.equalsIgnoreCase("GPS")){
	        	if(s_type == 0){ //LLSSLCSLs
	        		temp.setData(1,(BinaryParser.readNextLong(input,false)- 460000000)/6000000.0 + 46);
	        		temp.setData(2,(BinaryParser.readNextLong(input,false)- 60000000)/6000000.0 + 6);
	        		temp.setData(3,BinaryParser.readNextShort(input,false) / 10.0);
	        		temp.setData(4,BinaryParser.readNextShort(input, false)/ 100.0);			
	        		temp.setData(5,BinaryParser.readNextLong(input, false)/1000.0);
	        		temp.setData(6,BinaryParser.readNextChar(input, false));
	        		temp.setData(7,BinaryParser.readNextShort(input, false)/100);
	        		temp.setData(8,BinaryParser.readNextLong(input,false)/100.0);
	        		temp.setData(9,BinaryParser.readNextShort(input, true)*0.00390625);
	        		dataProduced(new StreamElement(temp));
				 }
	        } else if (data_type.equalsIgnoreCase("TL")){
	        	if(!last_values.containsKey(id) ){
	        		last_values.put(id,new StreamElement(structure, new Serializable[structure.length]));
	        		last_values.get(id).setData(0,id);
	        	}	        	
	        	last_values.get(id).setTimeStamp(time);
	        	if(s_type == 2){
	        		last_values.get(id).setData(1,BinaryParser.readNextChar(input, false));
				 }else if(s_type == 3){
					String[] s = new String((byte[]) streamElement.getData("payload")).split(",",2);
					last_values.get(id).setData(2,Short.parseShort(s[0]));
					last_values.get(id).setData(3, s[1]);
				 }else if(s_type == 4){
					String s = new String((byte[]) streamElement.getData("payload"));
					last_values.get(id).setData(4, s); 
				 }else if(s_type == 5){
					String s = new String((byte[]) streamElement.getData("payload"));
					last_values.get(id).setData(5, s);
				 }
	        	if (s_type > 1 && s_type < 6){
	        		dataProduced(new StreamElement(last_values.get(id)));
	        	}
	        }

		}catch(Exception e){
			logger.warn("error processing packet",e);
		}
	}
}

