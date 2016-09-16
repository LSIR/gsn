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
* File: app/providers/gsn/GSNLoginUsernamePasswordAuthUser.java
*
* @author Julien Eberle
*
*/
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
