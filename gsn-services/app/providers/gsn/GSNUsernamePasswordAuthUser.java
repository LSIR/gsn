package providers.gsn;

import providers.gsn.GSNUsernamePasswordAuthProvider.GSNSignup;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.NameIdentity;
import com.feth.play.module.pa.user.FirstLastNameIdentity;

public class GSNUsernamePasswordAuthUser extends UsernamePasswordAuthUser
		implements NameIdentity, FirstLastNameIdentity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String name;
	private final String firstName;
	private final String lastName;

	public GSNUsernamePasswordAuthUser(final GSNSignup signup) {
		super(signup.password, signup.email);
		this.name = signup.name;
		firstName = signup.firstname;
		lastName = signup.lastname;
		
	}
	
	public GSNUsernamePasswordAuthUser(final String email,final String password) {
		super(password, email);
		this.name = null;
		this.lastName = null;
		this.firstName = null;
	}

	/**
	 * Used for password reset only - do not use this to signup a user!
	 * @param password
	 */
	public GSNUsernamePasswordAuthUser(final String password) {
		super(password, null);
		name = null;
		this.lastName = null;
		this.firstName = null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	@Override
	public String getLastName() {
		return lastName;
	}
}
