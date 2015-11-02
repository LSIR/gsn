package providers.gsn;

import providers.gsn.GSNUsernamePasswordAuthProvider.GSNSignup;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.NameIdentity;

public class GSNUsernamePasswordAuthUser extends UsernamePasswordAuthUser
		implements NameIdentity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String name;

	public GSNUsernamePasswordAuthUser(final GSNSignup signup) {
		super(signup.password, signup.email);
		this.name = signup.name;
	}
	
	public GSNUsernamePasswordAuthUser(final String email,final String password) {
		super(password, email);
		this.name = null;
	}

	/**
	 * Used for password reset only - do not use this to signup a user!
	 * @param password
	 */
	public GSNUsernamePasswordAuthUser(final String password) {
		super(password, null);
		name = null;
	}

	@Override
	public String getName() {
		return name;
	}
}
