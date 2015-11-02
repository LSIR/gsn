package models.gsn.auth;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
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
	@ManyToOne
	public User user;
	public String code;
	public String redirect;
	@OneToMany(cascade = CascadeType.ALL)
	public List<OAuthToken> tokens;

	public static final AppModel.Finder<Long, Client> find = new AppModel.Finder<Long, Client>(
			Long.class, Client.class);



	public static Client findByName(String value) {
		return find.where().eq("name", value).findUnique();
	}
	
	public static Client findById(String value) {
		return find.where().eq("clientId", value).findUnique();
	}
	
	public static Client findByCode(String value) {
		return find.where().eq("code", value).findUnique();
	}
}
