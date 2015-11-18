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
