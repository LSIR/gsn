package models.gsn.auth;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import scala.util.Random;

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


	public static final AppModel.Finder<Long, OAuthToken> find = new AppModel.Finder<Long, OAuthToken>(
			Long.class, OAuthToken.class);


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
		t.duration = 36000L;
		t.refresh = UUID.randomUUID().toString();
		t.save();
		return t;
	}
}
