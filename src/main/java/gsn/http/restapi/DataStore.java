package gsn.http.restapi;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.data.*;
import gsn.http.ac.DataSource;
import gsn.storage.DataEnumerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

import org.apache.commons.collections.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.collection.JavaConversions;
import scala.collection.Seq;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class DataStore {
	private static transient Logger logger = LoggerFactory.getLogger(DataStore.class);
	private Config config=ConfigFactory.load();
	DateFormat dateFormat = new SimpleDateFormat(config.getString("dateFormat"));

	public Sensor findSensor(String vsName,boolean latestValues){
	    VSensorConfig sensorConfig = Mappings.getConfig(vsName);

        ArrayList<Field> fields = new ArrayList<Field>();
        
        fields.add(new Field("time",IntDT$.MODULE$,new DataUnit("s","s"),"time"));
        for (DataField out:sensorConfig.getOutputStructure()){              
      	  fields.add(new Field(out.getName(),DoubleDT$.MODULE$,
      			  new DataUnit(out.getUnit(),out.getUnit()),""));        	  
        }
        
        ArrayList<Object[]> values = new ArrayList<Object[]>();

        if (latestValues){          
      	  java.util.Map<String, Object> se = DataStore.latestValues(sensorConfig.getName());
            if (se != null){               
            	  Object[] vals=new Object[fields.size()];                	
             	  int i=1;
             	  vals[0]=se.get("time");
                //vals[0]=dateFormat.format(new Date((Long)vals[1]));
                for (DataField df: sensorConfig.getOutputStructure()){
                	  vals[i]=se.get(df.getName().toLowerCase());
                 	  i++;
                }
                values.add(vals);
            }
        }
        HashMap<String,String> props=new HashMap<String,String>();
        for ( KeyValue df : sensorConfig.getAddressing()){
        	props.put(df.getKey().toString().toLowerCase().trim(), df.getValue().toString().trim());
        }
  	  String is_public_res = "true";
    	  if (Main.getContainerConfig().isAcEnabled() && DataSource.isVSManaged(sensorConfig.getName())) 
    		  is_public_res = "false";
    	  props.put("is_public", is_public_res);          	  
        props.put("description", sensorConfig.getDescription());      

        Double lat=doubleOrNull(props.get("latitude"));
        Double lon=doubleOrNull(props.get("longitude"));
        Double alt=doubleOrNull(props.get("altitude"));
                  
        Sensor s=new Sensor(sensorConfig.getName(),
      		  JavaConversions.asScalaBuffer(fields),
      		  Location.apply(lat,lon,alt),
      		  JavaConversions.mapAsScalaMap(props),
      		  JavaConversions.asScalaBuffer(values));         
        return s;
	}
	
	public static java.util.Map<String, Object> latestValues(String vsName) {
		StringBuilder query = new StringBuilder("select * from ")
				.append(vsName)
				.append(" where timed = (select max(timed) from ")
				.append(vsName).append(")");
		java.util.Map<String, Object> toReturn = new HashMap<String, Object>();
		try {
			DataEnumerator result = Main.getStorage(vsName).executeQuery(query,
					true);
			if (result.hasMoreElements()) {
				StreamElement se = result.nextElement();
				toReturn.put("time", se.getTimeStamp());
				for (String fn : se.getFieldNames()) {
					toReturn.put(fn.toLowerCase(), (Double) se.getData(fn));
				}
			}
		} catch (SQLException e) {
			logger.error("ERROR IN EXECUTING, query: " + query);
			logger.error(e.getMessage(), e);
			return null;
		}
		return toReturn;
	}

	public Sensor query(String vsName, String[] fields,
			String[] conditions, long from, long to, int size) {
		Connection connection = null;
		ResultSet resultSet = null;		

		ArrayList<Object[]> res=new ArrayList<Object[]>();
		try {
			connection = Main.getStorage(vsName).getConnection();
			StringBuilder query = new StringBuilder("select timed");

			for (int i = 0; i < fields.length; i++) {
				query.append(", " + fields[i]);
			}
			query.append(" from ").append(vsName);
			//.append(" where timed >=").append(from).append(" and timed <=").append(to);
			if (conditions != null && conditions.length>0) {
				query.append(" where ");
				int i=0;
				for (String cond : conditions) {
					if (i>0) query.append(" and ");
					query.append(cond);
				}
			}

			if (size > 0) {
				query.append(" order by timed desc").append(" limit 0," + size);
			}

			resultSet = Main.getStorage(vsName).executeQueryWithResultSet(query, connection);
			
				while (resultSet.next()) {
					Object[] tuple=new Object[fields.length+1];
					tuple[0]=getObject(resultSet,"timed");
	                //tuple[0]=dateFormat.format(new Date((Long)tuple[1]));
					for (int i = 1; i < fields.length+1; i++) {
						tuple[i]=getObject(resultSet, fields[i-1]);
					}
					if (size>0)
						res.add(0, tuple);
					else
					    res.add(tuple);
				}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			throw new IllegalArgumentException("Invalid query.");
		} finally {
			Main.getStorage(vsName).close(resultSet);
			Main.getStorage(vsName).close(connection);
		}
		Sensor s=findSensor(vsName,false);
		
		ArrayList<String> ff=new ArrayList<String>();;
		ff.add("time");
		ff.addAll(Arrays.asList(fields));
		Seq<String> fNames=JavaConversions.asScalaBuffer(ff);
		Sensor sensorWithValues=Sensor.createWithValues(s,fNames,
				JavaConversions.asScalaBuffer(res));

		return sensorWithValues;
	}

	 private Object getObject(ResultSet rs,String fieldName) throws SQLException{
	    Object d=rs.getObject(fieldName);
        if (rs.wasNull()) return null;
	    else return d;
     }

	  private Double doubleOrNull(String doub){
		  if (doub==null) return null;
		  try {
			 Double d=Double.valueOf(doub);
			return d;		
		  } catch (NumberFormatException e){
			  return null;
		  }			  
	  }
	  
}
