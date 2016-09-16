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
* File: app/providers/gsn/GSNUsernamePasswordAuthUser.java
*
* @author Julien Eberle
*
*/
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
