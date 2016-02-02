package tinygsn.model.wrappers;

import java.sql.SQLException;
import java.util.ArrayList;


import java.util.Collections;
import java.util.Comparator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;

import tinygsn.beans.DataField;
import tinygsn.beans.StaticData;
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
	public Class<? extends WrapperService> getSERVICE() {
		return LocalService.class;
	}

	private DataField[] outputS = null;

	private long lastRun = System.currentTimeMillis();

	public String getWrapperName() {
		return this.getClass().getName() + "?" + getConfig().getParam();
	}

	@Override
	public DataField[] getOutputStructure() {
		if (outputS == null) {
			SqliteStorageManager storage = new SqliteStorageManager();
			try {
				outputS = storage.tableToStructure("vs_" + getConfig().getParam());
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
		SqliteStorageManager storage = new SqliteStorageManager();
		ArrayList<StreamElement> r = storage.executeQueryGetLatestValues("vs_" + getConfig().getParam(), getFieldList(), getFieldType(), 1000, lastRun);
		Collections.sort(r, new Comparator<StreamElement>() {
			@Override
			public int compare(StreamElement lhs, StreamElement rhs) {
				return Long.valueOf(lhs.getTimeStamp()).compareTo(rhs.getTimeStamp());
			}
		});

		for (StreamElement s : r) {
			postStreamElement(s);
			lastRun = s.getTimeStamp();
		}
	}

	@Override
	synchronized public boolean start() {
		getConfig().setRunning(true);
		return true;
	}

	public static boolean startLocal() {
		try {
			Intent serviceIntent = new Intent(StaticData.globalContext, LocalService.class);
			StaticData.globalContext.startService(serviceIntent);
			return true;
		} catch (Exception e) {
			// release anything?
		}
		return false;
	}

	@Override
	synchronized public boolean stop() {
		getConfig().setRunning(false);
		return true;
	}

	public static class LocalService extends WrapperService {

		public LocalService() {
			super("localService");

		}

		@Override
		protected void onHandleIntent(Intent intent) {
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			try {
				for (String s : StaticData.getLocalWrapperNames()) {
					w = StaticData.getWrapperByName(s);
					if (w.getConfig().isRunning()) {
						w.runOnce();
					}
				}
			} catch (Exception e1) {
			}
			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 10, PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		}

	}

}
