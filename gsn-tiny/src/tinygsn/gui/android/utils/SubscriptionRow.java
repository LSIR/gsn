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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/gui/android/utils/SubscriptionRow.java
*
* @author Do Ngoc Hoan
*/


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
