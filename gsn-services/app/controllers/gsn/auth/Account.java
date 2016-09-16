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
* File: app/controllers/gsn/auth/Account.java
*
* @author Julien Eberle
*
*/
package controllers.gsn.auth;

import models.gsn.auth.User;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import play.data.Form;
import play.data.format.Formats.NonEmpty;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import providers.gsn.GSNUsernamePasswordAuthProvider;
import providers.gsn.GSNUsernamePasswordAuthUser;
import views.html.account.*;

import controllers.gsn.auth.routes;

import static play.data.Form.form;

public class Account extends Controller {

	public static class Accept {

		@Required
		@NonEmpty
		public Boolean accept;

		public Boolean getAccept() {
			return accept;
		}

		public void setAccept(Boolean accept) {
			this.accept = accept;
		}

	}

	public static class PasswordChange {
		@MinLength(5)
		@Required
		public String password;

		@MinLength(5)
		@Required
		public String repeatPassword;

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getRepeatPassword() {
			return repeatPassword;
		}

		public void setRepeatPassword(String repeatPassword) {
			this.repeatPassword = repeatPassword;
		}

		public String validate() {
			if (password == null || !password.equals(repeatPassword)) {
				return Messages
						.get("playauthenticate.change_password.error.passwords_not_same");
			}
			return null;
		}
	}
	
	public static class EditProfile {
		@Required
		public String firstname;
		
		@Required
		public String lastname;

		public String getFirstname() {
			return firstname;
		}
		public EditProfile() {}

		public EditProfile(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}
	}

	private static final Form<Accept> ACCEPT_FORM = form(Accept.class);
	private static final Form<Account.PasswordChange> PASSWORD_CHANGE_FORM = form(Account.PasswordChange.class);
	private static final Form<Account.EditProfile> EDIT_PROFILE_FORM = form(Account.EditProfile.class);

	@SubjectPresent
	public static Result link() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		return ok(link.render());
	}

	@Restrict(@Group(LocalAuthController.USER_ROLE))
	public static Result verifyEmail() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final User user = LocalAuthController.getLocalUser(session());
		if (user.emailValidated) {
			// E-Mail has been validated already
			flash(LocalAuthController.FLASH_MESSAGE_KEY,
					Messages.get("playauthenticate.verify_email.error.already_validated"));
		} else if (user.email != null && !user.email.trim().isEmpty()) {
			flash(LocalAuthController.FLASH_MESSAGE_KEY, Messages.get(
					"playauthenticate.verify_email.message.instructions_sent",
					user.email));
			GSNUsernamePasswordAuthProvider.getProvider()
					.sendVerifyEmailMailingAfterSignup(user, ctx());
		} else {
			flash(LocalAuthController.FLASH_MESSAGE_KEY, Messages.get(
					"playauthenticate.verify_email.error.set_email_first",
					user.email));
		}
		return redirect(routes.LocalAuthController.profile());
	}

	@Restrict(@Group(LocalAuthController.USER_ROLE))
	public static Result changePassword() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final User u = LocalAuthController.getLocalUser(session());

		if (!u.emailValidated) {
			return ok(unverified.render());
		} else {
			return ok(password_change.render(PASSWORD_CHANGE_FORM));
		}
	}

	@Restrict(@Group(LocalAuthController.USER_ROLE))
	public static Result doChangePassword() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<Account.PasswordChange> filledForm = PASSWORD_CHANGE_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not select whether to link or not link
			return badRequest(password_change.render(filledForm));
		} else {
			final User user = LocalAuthController.getLocalUser(session());
			final String newPassword = filledForm.get().password;
			user.changePassword(new GSNUsernamePasswordAuthUser(newPassword),
					true);
			flash(LocalAuthController.FLASH_MESSAGE_KEY,
					Messages.get("playauthenticate.change_password.success"));
			return redirect(routes.LocalAuthController.profile());
		}
	}
	
	@Restrict(@Group(LocalAuthController.USER_ROLE))
	public static Result editProfile() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final User u = LocalAuthController.getLocalUser(session());

		if (!u.emailValidated) {
			return ok(unverified.render());
		} else {
			return ok(edit_profile.render(EDIT_PROFILE_FORM.fill(new EditProfile(u.firstName, u.lastName))));
		}
	}

	@Restrict(@Group(LocalAuthController.USER_ROLE))
	public static Result doEditProfile() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<Account.EditProfile> filledForm = EDIT_PROFILE_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not select whether to link or not link
			return badRequest(edit_profile.render(filledForm));
		} else {
			final User user = LocalAuthController.getLocalUser(session());
			user.firstName = filledForm.get().firstname;
			user.lastName = filledForm.get().lastname;
			user.update();
			flash(LocalAuthController.FLASH_MESSAGE_KEY,
					Messages.get("playauthenticate.edit_profile.success"));
			return redirect(routes.LocalAuthController.profile());
		}
	}

	@SubjectPresent
	public static Result askLink() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final AuthUser u = PlayAuthenticate.getLinkUser(session());
		if (u == null) {
			// account to link could not be found, silently redirect to login
			return redirect(routes.LocalAuthController.index());
		}
		return ok(ask_link.render(ACCEPT_FORM, u));
	}

	@SubjectPresent
	public static Result doLink() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final AuthUser u = PlayAuthenticate.getLinkUser(session());
		if (u == null) {
			// account to link could not be found, silently redirect to login
			return redirect(routes.LocalAuthController.index());
		}

		final Form<Accept> filledForm = ACCEPT_FORM.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not select whether to link or not link
			return badRequest(ask_link.render(filledForm, u));
		} else {
			// User made a choice :)
			final boolean link = filledForm.get().accept;
			if (link) {
				flash(LocalAuthController.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.accounts.link.success"));
			}
			return PlayAuthenticate.link(ctx(), link);
		}
	}

	@SubjectPresent
	public static Result askMerge() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		// this is the currently logged in user
		final AuthUser aUser = PlayAuthenticate.getUser(session());

		// this is the user that was selected for a login
		final AuthUser bUser = PlayAuthenticate.getMergeUser(session());
		if (bUser == null) {
			// user to merge with could not be found, silently redirect to login
			return redirect(routes.LocalAuthController.index());
		}

		// You could also get the local user object here via
		// User.findByAuthUserIdentity(newUser)
		return ok(ask_merge.render(ACCEPT_FORM, aUser, bUser));
	}

	@SubjectPresent
	public static Result doMerge() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		// this is the currently logged in user
		final AuthUser aUser = PlayAuthenticate.getUser(session());

		// this is the user that was selected for a login
		final AuthUser bUser = PlayAuthenticate.getMergeUser(session());
		if (bUser == null) {
			// user to merge with could not be found, silently redirect to login
			return redirect(routes.LocalAuthController.index());
		}

		final Form<Accept> filledForm = ACCEPT_FORM.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not select whether to merge or not merge
			return badRequest(ask_merge.render(filledForm, aUser, bUser));
		} else {
			// User made a choice :)
			final boolean merge = filledForm.get().accept;
			if (merge) {
				flash(LocalAuthController.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.accounts.merge.success"));
			}
			return PlayAuthenticate.merge(ctx(), merge);
		}
	}

}
