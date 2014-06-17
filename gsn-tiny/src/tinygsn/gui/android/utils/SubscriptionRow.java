package tinygsn.gui.android.utils;

import org.kroz.activerecord.ActiveRecordBase;

/**
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 * 
 */
public class SubscriptionRow extends ActiveRecordBase {
	public String server;
	public String vsname;
	public String data;
	public String read; // 0: unread

	public SubscriptionRow() {
	}

	public SubscriptionRow(String server, String vsname, String data, String read) {
		super();
		this.setServer(server);
		this.setVsname(vsname);
		this.setData(data);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		return sb.append(_id).append(" ").append(server).append(" ").append(vsname)
				.toString();
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getVsname() {
		return vsname;
	}

	public void setVsname(String vsname) {
		this.vsname = vsname;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getRead() {
		return read;
	}

	public void setRead(String read) {
		this.read = read;
	}

}
