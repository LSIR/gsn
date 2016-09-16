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
* File: app/models/gsn/auth/TokenAction.java
*
* @author Julien Eberle
*
*/
package models.gsn.auth;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.format.Formats;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.QueryIterator;
import com.avaje.ebean.annotation.EnumValue;

@Entity
public class TokenAction extends Model {

	public enum Type {
		@EnumValue("EV")
		EMAIL_VERIFICATION,

		@EnumValue("PR")
		PASSWORD_RESET
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Verification time frame (until the user clicks on the link in the email)
	 * in seconds
	 * Defaults to one week
	 */
	private final static long VERIFICATION_TIME = 7 * 24 * 3600;

	@Id
	public Long id;

	@Column(unique = true)
	public String token;

	@ManyToOne
	public User targetUser;

	public Type type;

	@Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
	public Date created;

	@Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
	public Date expires;

	public static final Finder<Long, TokenAction> find = new Finder<Long, TokenAction>(
			Long.class, TokenAction.class);

	public static TokenAction findByToken(final String token, final Type type) {
		return find.where().eq("token", token).eq("type", type).findUnique();
	}

	public static void deleteByUser(final User u, final Type type) {
		QueryIterator<TokenAction> iterator = find.where()
				.eq("targetUser.id", u.id).eq("type", type).findIterate();
		Ebean.delete(iterator);
		iterator.close();
	}

	public boolean isValid() {
		return this.expires.after(new Date());
	}

	public static TokenAction create(final Type type, final String token,
			final User targetUser) {
		final TokenAction ua = new TokenAction();
		ua.targetUser = targetUser;
		ua.token = token;
		ua.type = type;
		final Date created = new Date();
		ua.created = created;
		ua.expires = new Date(created.getTime() + VERIFICATION_TIME * 1000);
		ua.save();
		return ua;
	}
}
