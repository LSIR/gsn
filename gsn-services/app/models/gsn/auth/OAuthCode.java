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
* File: app/models/gsn/auth/OAuthCode.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import scala.util.Random;

@Entity
public class OAuthCode extends AppModel{
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
	public String code;
	public Long creation;
	
	public Client getClient(){
		return client;
	}


	public static final AppModel.Finder<Long, OAuthCode> find = new AppModel.Finder<Long, OAuthCode>(
			Long.class, OAuthCode.class);


	public static OAuthCode findByCode(String value) {
		return find.where().eq("code", value).findUnique();
	}
	
	public static OAuthCode generate(User user,Client client){
		OAuthCode t = new OAuthCode();
		t.user = user;
		t.client = client;
		t.code =  UUID.randomUUID().toString();
		t.creation = System.currentTimeMillis();
		t.save();
		return t;
	}
}
