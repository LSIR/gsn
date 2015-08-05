package tinygsn.model.wrappers;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;


public class RemoteWrapper extends AbstractWrapper {

	public RemoteWrapper(WrapperConfig wc) {
		super(wc);
	}
	public RemoteWrapper() {
	}

	@Override
	public Class<? extends WrapperService> getSERVICE() {return RemoteService.class;}
	
	private DataField[] outputS = null;
	private long lastRun = System.currentTimeMillis();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.ENGLISH);
	
    private HttpGet httpGet;
	private DefaultHttpClient httpclient = new DefaultHttpClient();
	
	private String url = "";
	private String vs_name = "";
	
	@Override
	public String[] getParameters(){
		return new String[]{"url","vs_name"};
		}
	
	@Override
	protected void initParameter(String key, String value){
		if (key.endsWith("url")){
			url = value;
		}else if (key.endsWith("vs_name")){
			vs_name = value;
		}
	}
	
	
	
	@Override
	public DataField[] getOutputStructure() {
		if (outputS == null){
			try{
				httpGet = new HttpGet("http://"+url+"/rest/sensors/"+vs_name+"?from=0000-00-00T00:00:00&to=0000-00-00T00:00:00&username=guest&password=guest");
				HttpResponse response = httpclient.execute(httpGet);
				int statusCode = response.getStatusLine().getStatusCode();
				InputStreamReader is = new InputStreamReader(response.getEntity().getContent(),"UTF-8");																				
				if (statusCode == 200) {
					BufferedReader bufferedReader = new BufferedReader(is);
			        String line = bufferedReader.readLine();
			        if(line != null){
			        	
			        	JSONObject obj = new JSONObject(line);
			        	JSONArray f = obj.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONArray("fields");
			        	outputS = new DataField[f.length()-1];
			        	for (int i = 1;i<f.length();i++){
			        		JSONObject v = f.getJSONObject(i);
			        		outputS[i-1] = new DataField(v.getString("name"), v.getString("type"));
			        	}
			        }
				}
				is.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
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
			String f = sdf.format(new Date(lastRun+1000));
			String t = sdf.format(new Date());
			httpGet = new HttpGet("http://"+url+"/rest/sensors/"+vs_name+"?from="+f+"&to="+t+"&username=guest&password=guest");
			Log.v("RemoteWrapper", "http://"+url+"/rest/sensors/"+vs_name+"?from="+f+"&to="+t+"&username=guest&password=guest");
			HttpResponse response = httpclient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			InputStreamReader is = new InputStreamReader(response.getEntity().getContent(),"UTF-8");																				
			if (statusCode == 200) {
				BufferedReader bufferedReader = new BufferedReader(is);
		        String line = bufferedReader.readLine();
		        if(line != null){
		        	JSONObject obj = new JSONObject(line);
		        	JSONArray val = obj.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONArray("values");
		        	for (int i = val.length()-1;i>=0;i--){
		        		StreamElement s = convertToSE(val.getJSONArray(i));
		        		if(s != null){
		        			postStreamElement(s);
		        			lastRun = Math.max(lastRun,s.getTimeStamp());
		        		}
		        	}
		        }
			}
			is.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	private StreamElement convertToSE(JSONArray jsonArray) throws JSONException {
		DataField[] struct = getOutputStructure();
		StreamElement s = new StreamElement(getOutputStructure(), new Serializable[struct.length]);
		for(int i=0;i<struct.length;i++){
			switch(struct[i].getDataTypeID()){
			case DataTypes.BIGINT:
				s.setData(i, jsonArray.getLong(i+1));
				break;
			case DataTypes.DOUBLE:
				s.setData(i, jsonArray.getDouble(i+1));
				break;
			case DataTypes.INTEGER:
				s.setData(i, jsonArray.getInt(i+1));
				break;
			default:
				s.setData(i, jsonArray.getString(i+1));	
			}
		}
		s.setTimeStamp((Long)s.getData("timestamp"));
		return s;
	}



	public static class RemoteService extends WrapperService{

		public RemoteService() {
			super("remoteService");

		}
	}

}
