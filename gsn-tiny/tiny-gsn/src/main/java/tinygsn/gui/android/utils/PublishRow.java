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

import java.io.Serializable;


public class PublishRow implements Serializable  {
	
	private static final long serialVersionUID = 7136228216822978309L;
	private int id;
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String serverurl;

	public String getServerSecret() {
		return serverSecret;
	}

	public void setServerSecret(String serverSecret) {
		this.serverSecret = serverSecret;
	}

	public String getServerID() {
		return serverID;
	}

	public void setServerID(String serverID) {
		this.serverID = serverID;
	}

	private String serverID;
	private String serverSecret;
	private boolean active;
	private String info;
	private String vsname;

	public String getServerurl() {
		return serverurl;
	}

	public void setServerurl(String serverurl) {
		this.serverurl = serverurl;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getVsname() {
		return vsname;
	}

	public void setVsname(String vsname) {
		this.vsname = vsname;
	}

	public PublishRow() {
	}

	public PublishRow(int id, String serverurl, String serverID, String serverSecret, boolean active,
			String info, String vsname) {
		this.id = id;
		this.serverurl = serverurl;
		this.serverID = serverID;
		this.serverSecret = serverSecret;
		this.active = active;
		this.info = info;
		this.vsname = vsname;
	}
	

}
