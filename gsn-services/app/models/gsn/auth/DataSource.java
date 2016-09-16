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
* File: app/models/gsn/auth/DataSource.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.UniqueConstraint;

import be.objectify.deadbolt.core.models.Permission;

/**
 * Initial version based on work by Steve Chaloner (steve@objectify.be) for
 * Deadbolt2
 */
@Entity
public class DataSource extends AppModel implements Permission {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	public Long id;
    
	public String value;
	
	public boolean is_public;

	@ManyToMany(mappedBy = "w_dataSources")
	@JoinTable(name="group_w_data_sources")
	public List<Group> w_groups;
	
	@ManyToMany(mappedBy = "w_dataSources")
	@JoinTable(name="user_w_data_sources")
	public List<User> w_users;
	
	@ManyToMany(mappedBy = "r_dataSources")
	@JoinTable(name="group_r_data_sources")
	public List<Group> r_groups;
	
	@ManyToMany(mappedBy = "r_dataSources")
	@JoinTable(name="user_r_data_sources")
	public List<User> r_users;
	
	public static play.db.ebean.Model.Finder<Long, DataSource> find = new play.db.ebean.Model.Finder<Long, DataSource>(
			Long.class, DataSource.class);

	public String getValue() {
		return value;
	}
	
	public boolean getIs_public(){
		return is_public;
	}
	
	public void setIs_public(boolean p){
		is_public=p;
	}

	public static DataSource findByValue(String value) {
		return find.where().eq("value", value).findUnique();
	}
}
