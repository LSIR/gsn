package providers.gsn;

import com.feth.play.module.pa.providers.password.DefaultUsernamePasswordAuthUser;

public class GSNLoginUsernamePasswordAuthUser extends
		DefaultUsernamePasswordAuthUser {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The session timeout in seconds
	 * Defaults to two weeks
	 */
	final static long SESSION_TIMEOUT = 24 * 14 * 3600;
	private long expiration;

	/**
	 * For logging the user in automatically
	 * 
	 * @param email
	 */
	public GSNLoginUsernamePasswordAuthUser(final String email) {
		this(null, email);
	}

	public GSNLoginUsernamePasswordAuthUser(final String clearPassword,
			final String email) {
		super(clearPassword, email);

		expiration = System.currentTimeMillis() + 1000 * SESSION_TIMEOUT;
	}

	@Override
	public long expires() {
		return expiration;
	}

}
