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
* File: app/models/gsn/auth/Client.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;


@Entity
public class Client extends AppModel{

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;
	public String name;
	public String clientId;
	public String secret;
	public String redirect;
	@OneToMany(cascade = CascadeType.ALL)
	public List<OAuthToken> tokens;
	@OneToMany(cascade = CascadeType.ALL)
	public List<OAuthCode> codes;
	@ManyToOne
	public User user;
	@ManyToMany
	public List<User> trusted_users;
	public Boolean linked = false;

	public static final play.db.ebean.Model.Finder<Long, Client> find = new play.db.ebean.Model.Finder<Long, Client>(
			Long.class, Client.class);

	public boolean isLinked() {
		return linked;
	}

	public void setLinked(boolean l) {
		linked = l;
	}
	
    public void setName(String n){
    	name = n;
    }

    public void setSecret(String s){
    	secret = s;
    }
    
    public void setClientId(String i){
    	clientId = i;
    }
    
    public String getClientId(){
    	return clientId;
    }
    
    public void setRedirect(String u){
    	redirect = u;
    }
    
    public String getRedirect(){
    	return redirect;
    }
    
    public void setUser(User r){
    	user = r;
    }
    
    public User getUser(){
    	return user;
    }
    
	public static Client findByName(String value) {
		return find.where().eq("name", value).findUnique();
	}
	
	public static Client findById(String value) {
		return find.where().eq("clientId", value).findUnique();
	}
	
	public static List<Client> findByUser(User u){
		return find.where().eq("user", u).findList();
	}
	
}
