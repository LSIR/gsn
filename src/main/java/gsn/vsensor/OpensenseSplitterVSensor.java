package gsn.vsensor;


import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
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
	        new DataField("LDSA","float"),
	        new DataField("idiff","float"),
	        new DataField("HV","smallint"),
	        new DataField("EM","float")});
		dataFields.put("FPH", new DataField[]{   
		    new DataField("station","smallint"),
	        new DataField("ufp_temp","float"),
	        new DataField("ufp_RH","smallint"),
	        new DataField("ufp_flow","float"),
	        new DataField("ufp_warning","smallint"),
	        new DataField("ufp_bat","float")});
	    dataFields.put("O3M", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("O3_res","smallint"),
	        new DataField("temp","float"),
	        new DataField("RH","smallint")});
	/*    dataFields.put("ACC", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("accel","binary")}); //can be read in python by calling: struct.unpack('>d',f.read(8))*/
	    dataFields.put("ACC", new DataField[]{
				new DataField("station","smallint"),
		        new DataField("x","smallint"),
		        new DataField("y","smallint"),
		        new DataField("z","smallint")});
	    dataFields.put("ANM", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("CO","smallint"),
	        new DataField("NO2","smallint"),
	        new DataField("CO2","smallint")});
	    dataFields.put("GPS", new DataField[]{
			new DataField("station","smallint"),
			new DataField("latitude","double"),
			new DataField("longitude","double"),
            new DataField("altitude","float"),
	        new DataField("speed","float"),
	        new DataField("satellites","smallint"),
	        new DataField("HDOP","float"),
	        new DataField("gyro","float")});
	    dataFields.put("TL", new DataField[]{
			new DataField("station","smallint"),
	        new DataField("door_state","smallint"),
	        new DataField("tl_line","smallint"),
	        new DataField("tl_destination","varchar(20)"),
	        new DataField("tl_curr_stop","varchar(20)"),
	        new DataField("tl_next_stop","varchar(20)")});
	    dataFields.put("ODO", new DataField[]{
	    	new DataField("station","smallint"),
	    	new DataField("odometer","smallint")});
	    dataFields.put("APM", new DataField[]{
	    	new DataField("station","smallint"),
	    	new DataField("pressure","int")});
	    dataFields.put("CSQ", new DataField[]{
	    	new DataField("station","smallint"),
	    	new DataField("signal","smallint")});
	    
	}
	
	private DataField[] structure;
	private String data_type;
	private StreamElement temp;
	private HashMap<Short,StreamElement> last_values;
	private HashMap<Short,ArrayList<Double>> last_acc;
	private HashMap<Short,Long> last_acc_time;
	
	private Long parsingErrorCount = 0L;

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
        	last_values = new HashMap<Short,StreamElement>(20);
        }else if(data_type.equalsIgnoreCase("ACC")){
        //	last_acc = new HashMap<Short, ArrayList<Double>>(20);
        //	last_acc_time = new HashMap<Short, Long>(20);
        }
		return true;
	}

	@Override
	public void dispose() {
	
	}
	
	@Override
	public Hashtable<String,Object> getStatistics() {
		Hashtable<String, Object> stat = super.getStatistics();
		stat.put("vs."+getVirtualSensorConfiguration().getName().replaceAll("\\.", "_") +".error.parsing.count", parsingErrorCount);
		return stat;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		try{
			short s_type = (Short) streamElement.getData("type");
			short id = (Short) streamElement.getData("station");
			if (s_type > 20) id += 200; //secondary sensors get another station id
			Long time = (Long) streamElement.getData("timestamp"); 
			temp.setData(0,id);
			temp.setTimeStamp(time);
			ByteArrayInputStream input = new ByteArrayInputStream((byte[]) streamElement.getData("payload"));
			BinaryParser p = new BinaryParser(input);
			if(data_type.equalsIgnoreCase("FPH")){
				 if(s_type == 12){//sSCCS
					 temp.setData(1,(float)(p.readNextShort(true)/10.0));
					 temp.setData(2,p.readNextShort(false));
					 temp.setData(3,(float)(p.readNextChar(false) / 100.0));
					 temp.setData(4,p.readNextChar(false));
					 temp.setData(5,(float)(p.readNextShort(false) / 100.0));
					 dataProduced(new StreamElement(temp));
				 } 
			} else if (data_type.equalsIgnoreCase("FPM")){
				 if(s_type == 13){ //LCSs
					 temp.setData(1,(float)(p.readNextLong(false) / 100.0));
					 temp.setData(2,(float)(p.readNextChar(false) / 10.0));
					 temp.setData(3,p.readNextShort(true));
					 temp.setData(4,(float)(p.readNextShort(true) / 100.0));
					 dataProduced(new StreamElement(temp));
				 }
			} else if (data_type.equalsIgnoreCase("APM")){
				if(s_type == 14){
					temp.setData(1, p.readNextLong(true));
					dataProduced(new StreamElement(temp));
				}
	        } else if (data_type.equalsIgnoreCase("O3M")){
	        	if(s_type == 10 || s_type == 30){//CSSC
	        		temp.setData(1,p.readNextShort(true));
	        		temp.setData(2,(float)(p.readNextShort(false)/10.0 - 40));
	        		temp.setData(3,p.readNextChar(false));
	        		dataProduced(new StreamElement(temp));
				 }       	
	        } else if (data_type.equalsIgnoreCase("ACC")){
	        	if(s_type == 6){//sss
	        		/*if (!last_acc_time.containsKey(id)){
	        			last_acc_time.put(id, time / 1000);
	        			last_acc.put(id, new ArrayList<Double>(4));
	        		}
	        		ArrayList<Double> a = last_acc.get(id);
	        		if (last_acc_time.get(id) != time / 1000){
	        			byte[] output = new byte[8*a.size()];
	        			int j = 0;
	        			for(Double d:a){
	        				long lng = Double.doubleToLongBits(d);
	        				for(int i = j; i < j+8; i++) output[i] = (byte)((lng >> ((7 - i) * 8)) & 0xff);
	        				j += 8;
	        			}
	        			temp.setData(1,output);
	        			temp.setTimeStamp(last_acc_time.get(id)*1000);
		        		dataProduced(new StreamElement(temp));
	        			last_acc_time.put(id, time / 1000);
	        			last_acc.put(id, new ArrayList<Double>(4));
	        			a = last_acc.get(id);
	        		}
	        		a.add(p.readNextShort(true)/1000.0);
	        		a.add(p.readNextShort(true)/1000.0);
	        		a.add(p.readNextShort(true)/-1000.0);
	        		*/
	        		temp.setData(1,p.readNextShort(true));
	        		temp.setData(2,p.readNextShort(true));
	        		temp.setData(3,p.readNextShort(true));
	        		dataProduced(new StreamElement(temp));
				 }
	        } else if (data_type.equalsIgnoreCase("ANM")){
	        	if(s_type == 9 || s_type == 29){ //SSS
	        		temp.setData(1,p.readNextShort(true));
	        		temp.setData(2,p.readNextShort(true));
	        		temp.setData(3,p.readNextShort(true));
	        		dataProduced(new StreamElement(temp)); 
				 }
	        } else if (data_type.equalsIgnoreCase("ODO")){
	        	if(s_type == 1){ //S
	        		temp.setData(1,(short)(p.readNextShort(false)-32768));
	        		dataProduced(new StreamElement(temp)); 
				 }
	        } else if (data_type.equalsIgnoreCase("CSQ")){
	        	if(s_type == 7){ //S
	        		temp.setData(1,(short)(p.readNextChar(false)));
	        		dataProduced(new StreamElement(temp)); 
				 }
	        } else if (data_type.equalsIgnoreCase("GPS")){
	        	if(s_type == 0){ //LLSSLCSLs
	        		if (((byte[])(streamElement.getData("payload"))).length > 6){
	        			temp.setData(1,(p.readNextLong(false)- 460000000)/6000000.0 + 46);
	        			temp.setData(2,(p.readNextLong(false)- 60000000)/6000000.0 + 6);
	        			temp.setData(3,(float)(p.readNextShort(false) / 10.0));
		        		temp.setData(4,(float)(p.readNextLong(false)/ 1000.0));			
		        		temp.setData(5,p.readNextChar(false));
		        		temp.setData(6,(float)(p.readNextShort(false)/100.0));
		        		temp.setData(7,(float)(p.readNextLong(false)/100.0));
	        		}
	        		else{
	        			temp.setData(1,(p.readNextLong(false)- 46000000)/600000.0 + 46);
	        			temp.setData(2,(p.readNextLong(false)- 6000000)/600000.0 + 6);
	        			temp.setData(3,(float)(p.readNextShort(false) / 10.0));
		        		temp.setData(4,null);			
		        		temp.setData(5,p.readNextChar(false));
		        		temp.setData(6,(float)(p.readNextShort(false)/10.0));
		        		temp.setData(7,null);
	        		}
                    validateRange("latitude",(Double)temp.getData("latitude"),46.6909,46.4660);
                    validateRange("longitude",(Double)temp.getData("longitude"),6.8289,6.5031);
                    validateRange("altitude",(Float)temp.getData("altitude"),1000f,300f);
                    validateRange("speed",(Float)temp.getData("speed"),150f,0f);
                    validateRange("satellites",(Integer)temp.getData("satellites"),20,0);
                    validateRange("HDOP",(Float)temp.getData("HDOP"),100f,0f);
                    validateRange("gyro",(Float)temp.getData("gyro"),45535f,20000f);
	        		dataProduced(new StreamElement(temp));
				 }
	        } else if (data_type.equalsIgnoreCase("TL")){
	        	if(!last_values.containsKey(id) ){
	        		last_values.put(id,new StreamElement(structure, new Serializable[structure.length]));
	        		last_values.get(id).setData(0,id);
	        	}	        	
	        	last_values.get(id).setTimeStamp(time);
	        	if(s_type == 2){
	        		last_values.get(id).setData(1,p.readNextChar(false));
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
			parsingErrorCount = parsingErrorCount == Long.MAX_VALUE ? 0 : parsingErrorCount + 1;
			logger.warn("error processing packet",e);
		}
	}
	
	private <T extends Comparable<T>> void validateRange(String field, T v, T low, T high) throws NumberFormatException{
		if (v.compareTo(high) > 0 ||v.compareTo(low) < 0) {
			throw new NumberFormatException("got value "+v+" for "+field+".");
		}
		
	}
}

