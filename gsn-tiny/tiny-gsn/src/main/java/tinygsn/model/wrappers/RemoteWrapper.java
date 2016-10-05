package tinygsn.model.wrappers;

import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import tinygsn.beans.Subscription;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.utils.Oauth2Connection;
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
	private Oauth2Connection connection;

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
				connection = new Oauth2Connection(sub.getUrl(), sub.getUsername(), sub.getPassword());
				connection.authenticate();
				StreamElement[] ses = StreamElement.fromJSON(connection.doJsonRequest("GET","/api/sensors/"+sub.getVsname()+"/data?from=0000-00-00T00:00:00&to=0000-00-00T00:00:00",""));
				if (ses != null && ses.length > 0) {
					outputS = new DataField[ses[0].getFieldNames().length];
					for (int i = 0; i < ses[0].getFieldNames().length; i++) {
						outputS[i] = new DataField(ses[0].getFieldNames()[i], ses[0].getFieldTypes()[i]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				outputS = null;
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
