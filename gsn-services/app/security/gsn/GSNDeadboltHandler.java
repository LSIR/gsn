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
* File: app/security/gsn/GSNDeadboltHandler.java
*
* @author Julien Eberle
*
*/
package security.gsn;


import models.gsn.auth.User;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Result;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;
import be.objectify.deadbolt.core.models.Subject;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUserIdentity;

public class GSNDeadboltHandler extends AbstractDeadboltHandler {

	@Override
	public Promise<Result> beforeAuthCheck(final Http.Context context) {
		if (PlayAuthenticate.isLoggedIn(context.session())) {
			// user is logged in
			return F.Promise.pure(null);
		} else {
			// user is not logged in

			// call this if you want to redirect your visitor to the page that
			// was requested before sending him to the login page
			// if you don't call this, the user will get redirected to the page
			// defined by your resolver
			final String originalUrl = PlayAuthenticate.storeOriginalUrl(context);
			
			System.out.println("-----------------"+originalUrl);

			context.flash().put("error",
					"You need to log in first, to view '" + originalUrl + "'");
            return F.Promise.promise(new F.Function0<Result>()
            {
                @Override
                public Result apply() throws Throwable
                {
                    return redirect(PlayAuthenticate.getResolver().login());
                }
            });
		}
	}

	@Override
	public Subject getSubject(final Http.Context context) {
		final AuthUserIdentity u = PlayAuthenticate.getUser(context);
		// Caching might be a good idea here
		return (Subject)User.findByAuthUserIdentity(u);
	}

	@Override
	public DynamicResourceHandler getDynamicResourceHandler(
			final Http.Context context) {
		return null;
	}

	@Override
	public F.Promise<Result> onAuthFailure(final Http.Context context,
			final String content) {
		// if the user has a cookie with a valid user and the local user has
		// been deactivated/deleted in between, it is possible that this gets
		// shown. You might want to consider to sign the user out in this case.
        return F.Promise.promise(new F.Function0<Result>()
        {
            @Override
            public Result apply() throws Throwable
            {
                return forbidden("Forbidden");
            }
        });
	}
}
