package tinygsn.model.wrappers;

import java.sql.SQLException;
import java.util.ArrayList;


import tinygsn.beans.DataField;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;
import tinygsn.storage.db.SqliteStorageManager;

public class LocalWrapper extends AbstractWrapper {

	public LocalWrapper(WrapperConfig wc) {
		super(wc);
	}
	public LocalWrapper() {
	}

	@Override
	public Class<? extends WrapperService> getSERVICE() {return LocalService.class;}
	
	private DataField[] outputS = null;
	
	private long lastRun = System.currentTimeMillis();

	public String getWrapperName() {
		return this.getClass().getName()+"?"+getConfig().getParam();
	}
	
	@Override
	public DataField[] getOutputStructure() {
		if (outputS == null){
			SqliteStorageManager storage = new SqliteStorageManager();
			try {
				outputS = storage.tableToStructure("vs_"+getConfig().getParam());
			} catch (SQLException e) {
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
		SqliteStorageManager storage = new SqliteStorageManager();
		ArrayList<StreamElement> r = storage.executeQueryGetLatestValues("vs_"+getConfig().getParam(), getFieldList(), getFieldType(), 1000, lastRun);
		for (StreamElement s : r){
			postStreamElement(s);
			lastRun = s.getTimeStamp();
		}
	}
	
	public static class LocalService extends WrapperService{

		public LocalService() {
			super("localService");

		}
	}

}
