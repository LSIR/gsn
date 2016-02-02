package tinygsn.model.wrappers;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import tinygsn.beans.DataField;
import tinygsn.beans.Subscription;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;


public class RemoteWrapper extends AbstractWrapper {

	public RemoteWrapper(WrapperConfig wc) {
		super(wc);
	}

	public RemoteWrapper() { }

    public String getWrapperName() {
        return this.getClass().getName() + "?" + getConfig().getParam();
    }

	@Override
	public Class<? extends WrapperService> getSERVICE() {
		return null;
	}

	private DataField[] outputS = null;
	private Subscription sub = null;
	private HttpGet httpGet;
	private DefaultHttpClient httpclient = new DefaultHttpClient();

	@Override
	public void initialize() {
		String value = getConfig().getParam();
        SqliteStorageManager storage = new SqliteStorageManager();
        sub = storage.getSubscribeInfo(Integer.parseInt(value));
	}

	@Override
	public DataField[] getOutputStructure() {
		if (outputS == null) {
			try {
				httpGet = new HttpGet("http://" + sub.getUrl() + "/rest/sensors/" + sub.getVsname() + "?from=0000-00-00T00:00:00&to=0000-00-00T00:00:00");
				HttpResponse response = httpclient.execute(httpGet);
				int statusCode = response.getStatusLine().getStatusCode();
				InputStreamReader is = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
				if (statusCode == 200) {
					BufferedReader bufferedReader = new BufferedReader(is);
					String line = bufferedReader.readLine();
					if (line != null) {
						JSONObject obj = new JSONObject(line);
						JSONArray f = obj.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONArray("fields");
						outputS = new DataField[f.length() - 2];
						for (int i = 2; i < f.length(); i++) {
							JSONObject v = f.getJSONObject(i);
							outputS[i - 2] = new DataField(v.getString("name"), v.getString("type"));
						}
					}
				}
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return outputS;
	}

	@Override
	public String[] getFieldList() {
		DataField[] df = getOutputStructure();
		String[] field = new String[df.length];
		for (int i = 0; i < df.length; i++) {
			field[i] = df[i].getName();
		}
		return field;
	}

	@Override
	public Byte[] getFieldType() {
		DataField[] df = getOutputStructure();
		Byte[] field = new Byte[df.length];
		for (int i = 0; i < df.length; i++) {
			field[i] = df[i].getDataTypeID();
		}
		return field;
	}

	@Override
	public void runOnce() {
        //based on the Subscription loop
	}

	@Override
	synchronized public boolean start() {
		getConfig().setRunning(true);
		return true;
	}

	@Override
	synchronized public boolean stop() {
		getConfig().setRunning(false);
		return true;
	}

}
