package gsn.msr.sensormap;

import gsn.Main;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;

public class MetaData {
	private String sensorName;
	private String sensorType;
	private String metadata;
	private String comments;
	private String unit;
	private String key;
	
	public String getSensorName() {
		String to_return = sensorName.replace("\"", "").trim();
		return to_return;
	}
	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}
	public String getSensorType() {
		String to_return = sensorType.replace("\"", "").trim();
		return to_return;
	}
	public void setSensorType(String sensorType) {
		this.sensorType = sensorType;
	}
	public String getMetadata() {
		String to_return = metadata.replace("\"", "").trim();
		return to_return;
	}
	public void setMetadata(String metaData) {
		this.metadata = metaData;
	}
	public String getComments() {
		String to_return = comments.replace("\"", "").trim();
		return to_return;
	}
	public void setComments(String comments) {
		String to_return = comments.replace("\"", "").trim();
		this.comments = to_return;
	}
	public String getUnit() {
		String to_return = unit.replace("\"", "").trim();
		return to_return;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[Key:").append(getKey()).append("\n");
		sb.append("Sensor-name:").append(getSensorName()).append("\n");
		sb.append("Sensor-Type:").append(getSensorType()).append("\n");
		sb.append("Metadata:").append(getMetadata()).append("\n");
		sb.append("Comment:").append(getComments()).append("\n");
		sb.append("Unit:").append(getUnit()).append("]\n");
		return sb.toString();
	}
	public static HashMap<String, MetaData> createMetaData(String path) throws FileNotFoundException{
		HashMap<String, MetaData> to_return = new HashMap<String, MetaData>();
		ColumnPositionMappingStrategy strat = new ColumnPositionMappingStrategy();
	    strat.setType(MetaData.class);
	    String[] columns = new String[] {"key","sensorName", "sensorType", "metadata", "comments", "unit"}; // the fields to bind do in your JavaBeab
        strat.setColumnMapping(columns);
	    CsvToBean csv = new CsvToBean();
	    List<MetaData> list = csv.parse(strat, new FileReader(path));
	    for (MetaData m : list) {
	    	to_return.put(m.getKey().toLowerCase().trim(), m);
//	    	System.out.println(m.toString());
	    }
		return to_return;
	}
	
	public static void main(String args[]) throws FileNotFoundException {
		HashMap<String, MetaData> output = createMetaData(Main.getContainerConfig().getMsrMap().get("metadata"));
	}
	
	
}
