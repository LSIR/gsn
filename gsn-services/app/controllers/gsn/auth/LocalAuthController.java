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
* File: app/controllers/gsn/auth/LocalAuthController.java
*
* @author Julien Eberle
*
*/
package controllers.gsn.auth;

import java.text.SimpleDateFormat;
import java.util.Date;

import models.gsn.auth.User;
import play.Routes;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.*;
import play.mvc.Http.Session;
import play.mvc.Result;
import providers.gsn.GSNUsernamePasswordAuthProvider;
import providers.gsn.GSNUsernamePasswordAuthProvider.GSNLogin;
import providers.gsn.GSNUsernamePasswordAuthProvider.GSNSignup;

import providers.gsn.GSNUsernamePasswordAuthUser;
import views.html.*;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.user.AuthUser;

public class LocalAuthController extends Controller {

	public static final String FLASH_MESSAGE_KEY = "message";
	public static final String FLASH_ERROR_KEY = "error";
	public static final String USER_ROLE = "user";
	public static final String ADMIN_ROLE = "admin";
	
	public static Result index() {
		return ok(index.render());
	}

	public static User getLocalUser(final Session session) {
		final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
		final User localUser = User.findByAuthUserIdentity(currentAuthUser);
		return localUser;
	}

	@Restrict(@Group(LocalAuthController.USER_ROLE))
	public static Result profile() {
		final User localUser = getLocalUser(session());
		return ok(profile.render(localUser));
	}

	public static Result login() {
		return ok(login.render(GSNUsernamePasswordAuthProvider.LOGIN_FORM));
	}

	public static Result doLogin() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<GSNLogin> filledForm = GSNUsernamePasswordAuthProvider.LOGIN_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			return badRequest(login.render(filledForm));
		} else {
			// Everything was filled
			return UsernamePasswordAuthProvider.handleLogin(ctx());
		}
	}

	public static Result signup() {
		return ok(signup.render(GSNUsernamePasswordAuthProvider.SIGNUP_FORM));
	}

	public static Result jsRoutes() {
		return ok(
				Routes.javascriptRouter("jsRoutes",
						controllers.gsn.auth.routes.javascript.Signup.forgotPassword()))
				.as("text/javascript");
	}

	public static Result doSignup() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<GSNSignup> filledForm = GSNUsernamePasswordAuthProvider.SIGNUP_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			return badRequest(signup.render(filledForm));
		} else {
			// Everything was filled
			// do something with your part of the form before handling the user
			// signup
			return UsernamePasswordAuthProvider.handleSignup(ctx());
		}
	}

	public static Result adduser() {
		return ok(adduser.render(GSNUsernamePasswordAuthProvider.SIGNUP_FORM));
	}

	public static Result doAdduser() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<GSNSignup> filledForm = GSNUsernamePasswordAuthProvider.SIGNUP_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			return badRequest(adduser.render(filledForm));
		} else {
			GSNUsernamePasswordAuthUser guser = new GSNUsernamePasswordAuthUser(filledForm.get());
			final User u = User.findByUsernamePasswordIdentity(guser);
			if (u != null) {
				flash(LocalAuthController.FLASH_ERROR_KEY,
						Messages.get("playauthenticate.user.exists.message"));
				return badRequest(adduser.render(filledForm));
			}
			// The user either does not exist or is inactive - create a new one
			// manually created users are directly validated
			@SuppressWarnings("unused")
			final User newUser = User.create(guser);
			newUser.emailValidated = true;
			newUser.save();
			return ok(adduser.render(GSNUsernamePasswordAuthProvider.SIGNUP_FORM));
		}
	}

	public static String formatTimestamp(final long t) {
		return new SimpleDateFormat("yyyy-dd-MM HH:mm:ss").format(new Date(t));
	}

}