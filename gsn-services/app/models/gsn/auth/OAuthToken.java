/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: app/models/gsn/auth/OAuthToken.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class OAuthToken extends AppModel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	public Long id;
	@ManyToOne
	public User user;
	@ManyToOne
	public Client client;
	public String token;
	public String refresh;
	public Long creation;
	public Long duration;

	public Client getClient(){
		return client;
	}

	public static final AppModel.Finder<Long, OAuthToken> find = new AppModel.Finder<Long, OAuthToken>(
			Long.class, OAuthToken.class);
	
	public static List<OAuthToken> findByUserClient(User u, Client c) {
		return find.where().eq("user", u).eq("client", c).findList();
	}

	public static OAuthToken findByToken(String value) {
		return find.where().eq("token", value).findUnique();
	}
	
	public static OAuthToken findByRefresh(String value) {
		return find.where().eq("refresh", value).findUnique();
	}
	
	public static OAuthToken generate(User user,Client client){
		OAuthToken t = new OAuthToken();
		t.user = user;
		t.client = client;
		t.token =  UUID.randomUUID().toString();
		t.creation = System.currentTimeMillis();
		t.duration = 600000L;
		t.refresh = UUID.randomUUID().toString();
		t.save();
		return t;
	}
}
