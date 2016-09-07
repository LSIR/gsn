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
