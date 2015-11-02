package models.gsn.auth;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;


@Entity
@Table(name = Group.tableName)
public class Group extends AppModel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String tableName = "groups";

	@Id
	public Long id;

	public String name;
	
	@ManyToMany
	public List<DataSource> dataSources;

	public static final AppModel.Finder<Long, Group> find = new AppModel.Finder<Long, Group>(
			Long.class, Group.class);

	public String getName() {
		return name;
	}

	public static Group findByName(String name) {
		return find.where().eq("name", name).findUnique();
	}
}
