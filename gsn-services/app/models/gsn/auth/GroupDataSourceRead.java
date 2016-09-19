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
* File: app/models/gsn/auth/GroupDataSourceRead.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Initial version based on work by Steve Chaloner (steve@objectify.be) for
 * Deadbolt2
 */
@Entity
@Table(name="group_data_source_read",  uniqueConstraints={
		   @UniqueConstraint(columnNames={"group", "data_source"})
		})
public class GroupDataSourceRead extends AppModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	public Long id;
    
	@ManyToOne
	public Group group;
	
	@ManyToOne
	public DataSource data_source;
	
	public static play.db.ebean.Model.Finder<Long, GroupDataSourceRead> find = new play.db.ebean.Model.Finder<Long, GroupDataSourceRead>(
			Long.class, GroupDataSourceRead.class);

	public static List<GroupDataSourceRead> findByUser(Group value) {
		return find.where().eq("group", value).findList();
	}
	
	public static List<GroupDataSourceRead> findByDataSource(DataSource value) {
		return find.where().eq("data_source", value).findList();
	}
	
	public static GroupDataSourceRead findByBoth(Group g, DataSource d) {
		return find.where().eq("group", g).eq("data_source", d).findUnique();
	}

}
