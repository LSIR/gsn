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
* File: app/models/gsn/auth/LinkedAccount.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.feth.play.module.pa.user.AuthUser;

@Entity
public class LinkedAccount extends AppModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@ManyToOne
	public User user;

	public String providerUserId;
	public String providerKey;

	public static final Finder<Long, LinkedAccount> find = new Finder<Long, LinkedAccount>(
			Long.class, LinkedAccount.class);

	public static LinkedAccount findByProviderKey(final User user, String key) {
		return find.where().eq("user", user).eq("providerKey", key)
				.findUnique();
	}

	public static LinkedAccount create(final AuthUser authUser) {
		final LinkedAccount ret = new LinkedAccount();
		ret.update(authUser);
		return ret;
	}
	
	public void update(final AuthUser authUser) {
		this.providerKey = authUser.getProvider();
		this.providerUserId = authUser.getId();
	}

	public static LinkedAccount create(final LinkedAccount acc) {
		final LinkedAccount ret = new LinkedAccount();
		ret.providerKey = acc.providerKey;
		ret.providerUserId = acc.providerUserId;

		return ret;
	}
}