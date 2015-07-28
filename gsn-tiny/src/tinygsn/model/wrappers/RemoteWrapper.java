package tinygsn.model.wrappers;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;


public class RemoteWrapper extends AbstractWrapper {

	public RemoteWrapper(WrapperConfig wc) {
		super(wc);
	}

	@Override
	public Class<? extends WrapperService> getSERVICE() {return RemoteService.class;}
	
	private DataField[] outputS = null;
	private long lastRun = System.currentTimeMillis();
	
	private String[] lastHeader;
	
    private HttpGet httpGet;
	private DefaultHttpClient httpclient = new DefaultHttpClient();
	
	private String url = "";
	private String vs_name = "";
	
	public String[] getParameters(){
		return new String[]{"url","vs_name"};
		}
	
	private void initParameter(String key, String value){
		if (key.endsWith("url")){
			url = value;
		}else if (key.endsWith("vs_name")){
			vs_name = value;
		}
	}
	
	
	
	@Override
	public DataField[] getOutputStructure() {
		if (outputS == null){
			
		}
		return outputS;
	}

	@Override
	public String[] getFieldList() {
		DataField[] df = getOutputStructure();
		String[] field = new String[df.length];
		for (int i=0;i<df.length;i++){
			field[i] = df[i].getName();
		}
		return field;
	}

	@Override
	public Byte[] getFieldType() {
		DataField[] df = getOutputStructure();
		Byte[] field = new Byte[df.length];
		for (int i=0;i<df.length;i++){
			field[i] = df[i].getDataTypeID();
		}
		return field;
	}

	@Override
	public void runOnce() {
		try{
			httpGet = new HttpGet("http://"+url+"/rest/sensors/"+vs_name+"?from=2013-05-30T08:30:04&to=2013-05-30T08:35:34&username=john2&password=john");
			HttpResponse response = httpclient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			InputStreamReader is = new InputStreamReader(response.getEntity().getContent(),"UTF-8");																				
			if (statusCode != 200) {
				BufferedReader bufferedReader = new BufferedReader(is);
		        String line = bufferedReader.readLine();
		        while(line != null){
		            
		        	StreamElement s = convertToSE(line);
		        	
		        	if(s != null){
		        	    postStreamElement(s);
					    lastRun = s.getTimeStamp();
		        	}
		            line = bufferedReader.readLine();
		        }
			}
			is.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	private StreamElement convertToSE(String line) {
		if (line.startsWith("#")){
			lastHeader = line.split("[#,]");
			return null;
		}
		if (lastHeader != null){
			DataField[] struct = getOutputStructure();
			String[] v = line.split(",");
			StreamElement s = new StreamElement(getOutputStructure(), new Serializable[struct.length]);
			for(int i=0;i<struct.length;i++){
				switch(struct[i].getDataTypeID()){
				case DataTypes.DOUBLE:
					s.setData(lastHeader[i], Double.parseDouble(v[i]));
					break;
				case DataTypes.INTEGER:
					s.setData(lastHeader[i], Integer.parseInt(v[i]));
					break;
				default:
					s.setData(lastHeader[i], v[i]);	
				}
			}
			return s;
		}
		return null;
	}



	public static class RemoteService extends WrapperService{

		public RemoteService() {
			super("remoteService");

		}
	}

}
