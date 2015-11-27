/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 *
 * This file is part of GSN.
 *
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GSN.  If not, see <http://www.gnu.org/licenses/>.
 *
 * File: src/gsn/http/restapi/VirtualSensor.java
 *
 * @author Milos Stojanovic
 *
 */

package gsn.http.restapi;

import gsn.beans.DataField;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class VirtualSensor {
	
    private static transient Logger logger = LoggerFactory.getLogger(VirtualSensor.class);

	private Map<String, String> metadata = new LinkedHashMap<String, String>();
	private List<DataField> fields = new ArrayList<DataField>();
	private List<Vector<Double>> values = new ArrayList<Vector<Double>>();
	private Vector<Long> timestamps = new Vector<Long>();
    private List<Double> latestValues = new ArrayList<Double>();
	
	public void addMetadata(String name, String value){
		metadata.put(name, value);
	}
	
	public Map<String, String> getMetadata(){
		return metadata;
	}
	
	public String getMetadataVal(String name){
		return metadata.get(name);
	}
	
	public void setMetadata(Map<String, String> metadata){
		this.metadata = metadata;
	}
	
	public void appendFields(DataField [] fields){
		for (DataField field: fields){
			this.fields.add(field);
		}
	}
	
	public void appendField(DataField field){
		this.fields.add(field);
	}
	
	public void setFields(List<DataField> fields){
		this.fields = fields;
	}
	
	public List<DataField> getFields(){
		return fields;
	}

	public List<Vector<Double>> getValues() {
		return values;
	}
	
	public Vector<Long> getTimestamps() {
		return timestamps;
	}

    public List<Double> getLatestValues() { return latestValues; }

    public void setLatestValues(List<Double> latestValues) {
        this.latestValues = latestValues;
    }

    public void addLatestValue(Double latestValue) {
        latestValues.add(latestValue);
    }

	public void setValues(List<Vector<Double>> values, Vector<Long> timestamps) {
		this.values = values;
		this.timestamps = timestamps;
	}
	
	public void setValues(Vector<Double> values, long timestamp, int index) {
		this.values.add(index, values);
		this.timestamps.add(index, timestamp);
	}
	
	public static String generateFileContent(List<VirtualSensor> listSens, String format){
    	if (RestServlet.FORMAT_CSV.equals(format)) return generateCSVFileContent(listSens);
        else if (RestServlet.FORMAT_JSON.equals(format)) return generateJSONFileContent(listSens);
        else if (RestServlet.FORMAT_GEOJSON.equals(format)) return generateGeoJSONFileContent(listSens);
        else return null;
    }
	
	private static String generateJSONFileContent(List<VirtualSensor> listOfSensors){
    	JSONArray sensorsInfo = new JSONArray();
    	for (VirtualSensor sensor: listOfSensors){
    		JSONObject sensorInfo = new JSONObject();
    		
    		//header
    		Iterator<Map.Entry<String, String>> metadataIterator = sensor.getMetadata().entrySet().iterator();
    		while (metadataIterator.hasNext()){
    			Map.Entry<String, String> elem = metadataIterator.next();
    			sensorInfo.put(elem.getKey(), elem.getValue());
    		}
    		
    		//fields, units, types
    		JSONArray setOfFields = new JSONArray();
    		for (DataField field: sensor.getFields()){    			
    			String unit = field.getUnit();
    			if (unit == null || unit.trim().length() == 0) unit = "";
    			
    			JSONObject fieldInfo = new JSONObject();
    			fieldInfo.put(RequestHandler.getConst("NAME"), field.getName().toLowerCase());
    			fieldInfo.put(RequestHandler.getConst("UNIT"), unit);
    			fieldInfo.put(RequestHandler.getConst("TYPE"), field.getType().toLowerCase());
    			setOfFields.add(fieldInfo);
    		}
    		sensorInfo.put(RequestHandler.getConst("FIELDS"), setOfFields);
    		
    		//values
    		Vector<Long> timestamps = sensor.getTimestamps();
    		if (timestamps.size() > 0){
                JSONArray setOfValues = new JSONArray();
                List<Vector<Double>> values = sensor.getValues();
	    		for (int i = 0; i < timestamps.size(); i++){
	    			JSONArray valuesForOneField = new JSONArray();
	    			valuesForOneField.add((new java.text.SimpleDateFormat(RequestHandler.getConst("ISO_FORMAT")).format(new java.util.Date(timestamps.get(i)))).toString().replace('T', ' '));
	    			valuesForOneField.add(timestamps.get(i));
	    			for (Object value: values.get(i)){
	    				valuesForOneField.add(value);
	    			}
	    			setOfValues.add(valuesForOneField);
	    		}
	    		sensorInfo.put(RequestHandler.getConst("VALUES"), setOfValues);
    		}

            //latest values
            List<Double> latestValues = sensor.getLatestValues();
            if (latestValues.size() > 0){
                JSONArray setOfValues = new JSONArray();
                JSONArray valueForOneField = new JSONArray();
                for (Object value: latestValues){
                    valueForOneField.add(value);
                }
                setOfValues.add(valueForOneField);
                sensorInfo.put(RequestHandler.getConst("VALUES"), setOfValues);
            }
    		
    		sensorsInfo.add(sensorInfo);
    	}
    	return sensorsInfo.toJSONString();
    }
	
	private static String generateCSVFileContent(List<VirtualSensor> listOfSensors){
    	StringBuilder file = new StringBuilder("");
    	for (VirtualSensor sensor: listOfSensors){
    		
    		//header
    		Iterator<Map.Entry<String, String>> metadataIterator = sensor.getMetadata().entrySet().iterator();
    		while (metadataIterator.hasNext()){
    			Map.Entry<String, String> elem = metadataIterator.next();
    			file.append("# " + elem.getKey() + ":" + elem.getValue() + "\n");
    		}
    		
    		//fields, units, types
    		StringBuilder field_names = new StringBuilder("# " + RequestHandler.getConst("FIELDS") + ":");
    		StringBuilder field_units = new StringBuilder("# " + RequestHandler.getConst("UNITS") + ":");
    		StringBuilder field_types = new StringBuilder("# " + RequestHandler.getConst("TYPES") + ":");
    		for (DataField field: sensor.getFields()){
    		
    			String unit = field.getUnit();
    			if (unit == null || unit.trim().length() == 0) unit = "";
    			if (field != sensor.getFields().get(sensor.getFields().size() - 1)){
    				field_names.append(field.getName().toLowerCase() + ",");
        			field_units.append(unit + ",");
        			field_types.append(field.getType().toLowerCase() + ",");
    			}else {
    				field_names.append(field.getName().toLowerCase() + "\n");
        			field_units.append(unit + "\n");
        			field_types.append(field.getType().toLowerCase() + "\n");
    			}
    		}
    		file.append(field_names.toString() + field_units.toString() + field_types.toString());
    		
    		//values
    		List<Vector<Double>> values = sensor.getValues();
    		Vector<Long> timestamps = sensor.getTimestamps();
    		for (int i = 0; i < timestamps.size(); i++){
    			file.append((new java.text.SimpleDateFormat(RequestHandler.getConst("ISO_FORMAT")).format(new java.util.Date(timestamps.get(i)))).toString().replace('T', ' ') + "," + timestamps.get(i));
    			boolean first = true;
                for (Object value: values.get(i)){
                    if (first){
                        first = false;
                    } else {
                        file.append(",");
                    }
                    file.append("," + value);
    			}
    			file.append("\n");
    		}

            //latest values
            List<Double> latestValues = sensor.getLatestValues();
            if (latestValues.size() > 0){
                for (Object value: latestValues){
                    file.append("," + value);
                }
                file.append("\n");
            }
    	}
    	return file.toString();
    }


    private static String generateGeoJSONFileContent(List<VirtualSensor> listOfSensors){
        JSONObject featureCollection = new JSONObject();
        featureCollection.put(RequestHandler.getConst("TYPE_GEOJSON"), RequestHandler.getConst("FEATURE_COLLECTION"));
        JSONArray features = new JSONArray();

        for (VirtualSensor sensor: listOfSensors){
            JSONObject feature = new JSONObject();
            feature.put(RequestHandler.getConst("TYPE_GEOJSON"), RequestHandler.getConst("FEATURE"));
            JSONObject geometry = new JSONObject();
            geometry.put(RequestHandler.getConst("TYPE_GEOJSON"), RequestHandler.getConst("POINT"));
            JSONArray coordinates = new JSONArray();
            JSONObject properties = new JSONObject();

            double longitude = 0;
            double latitude = 0;
            double altitude = -1;
            Iterator<Map.Entry<String, String>> metadataIterator = sensor.getMetadata().entrySet().iterator();
            while (metadataIterator.hasNext()){
                Map.Entry<String, String> elem = metadataIterator.next();
                try { 
	                if (RequestHandler.getConst("LONGITUDE").equalsIgnoreCase(elem.getKey())) 
	                	longitude = Double.parseDouble(elem.getValue());
	                else if (RequestHandler.getConst("LATITUDE").equalsIgnoreCase(elem.getKey())) 
	                	latitude = Double.parseDouble(elem.getValue());
	                else if (RequestHandler.getConst("ALTITUDE").equalsIgnoreCase(elem.getKey())) 
	                	altitude = Double.parseDouble(elem.getValue());
	                else properties.put(elem.getKey(), elem.getValue());
                } catch (NumberFormatException e){
                	logger.warn("Error parsing coordinates: "+e.getMessage(),e);
                }
            }
            coordinates.add(longitude);
            coordinates.add(latitude);
            if (altitude != -1) coordinates.add(altitude);

            //fields, units, types
            JSONArray setOfFields = new JSONArray();
            for (DataField field: sensor.getFields()){
                String unit = field.getUnit();
                if (unit == null || unit.trim().length() == 0) unit = "";

                JSONObject fieldInfo = new JSONObject();
                fieldInfo.put(RequestHandler.getConst("NAME"), field.getName().toLowerCase());
                fieldInfo.put(RequestHandler.getConst("UNIT"), unit);
                fieldInfo.put(RequestHandler.getConst("TYPE"), field.getType().toLowerCase());
                setOfFields.add(fieldInfo);
            }
            properties.put(RequestHandler.getConst("FIELDS"), setOfFields);

            //values
            Vector<Long> timestamps = sensor.getTimestamps();
            if (timestamps.size() > 0){
                JSONArray setOfValues = new JSONArray();
                List<Vector<Double>> values = sensor.getValues();
                for (int i = 0; i < timestamps.size(); i++){
                    JSONArray valuesForOneField = new JSONArray();
                    valuesForOneField.add((new java.text.SimpleDateFormat(RequestHandler.getConst("ISO_FORMAT")).format(new java.util.Date(timestamps.get(i)))).toString().replace('T', ' '));
                    valuesForOneField.add(timestamps.get(i));
                    for (Object value: values.get(i)){
                        valuesForOneField.add(value);
                    }
                    setOfValues.add(valuesForOneField);
                }
                properties.put(RequestHandler.getConst("VALUES"), setOfValues);
            }

            geometry.put(RequestHandler.getConst("COORDINATES"), coordinates);
            feature.put(RequestHandler.getConst("GEOMETRY"), geometry);
            feature.put(RequestHandler.getConst("PROPERTIES"), properties);

            features.add(feature);

            //latest values
            List<Double> latestValues = sensor.getLatestValues();
            if (latestValues.size() > 0){
                JSONArray setOfValues = new JSONArray();
                JSONArray valueForOneField = new JSONArray();
                for (Object value: latestValues){
                    valueForOneField.add(value);
                }
                setOfValues.add(valueForOneField);
                properties.put(RequestHandler.getConst("VALUES"), setOfValues);
            }
        }


        featureCollection.put(RequestHandler.getConst("FEATURES"), features);

        return featureCollection.toJSONString();
    }
}
