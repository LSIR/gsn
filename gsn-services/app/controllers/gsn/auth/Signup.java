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
* File: app/controllers/gsn/auth/Signup.java
*
* @author Julien Eberle
*
*/
package controllers.gsn.auth;

import models.gsn.auth.TokenAction;
import models.gsn.auth.TokenAction.Type;
import models.gsn.auth.User;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import providers.gsn.GSNLoginUsernamePasswordAuthUser;
import providers.gsn.GSNUsernamePasswordAuthProvider;
import providers.gsn.GSNUsernamePasswordAuthProvider.GSNIdentity;
import providers.gsn.GSNUsernamePasswordAuthUser;
import views.html.account.signup.*;

import com.feth.play.module.pa.PlayAuthenticate;

import static play.data.Form.form;

public class Signup extends Controller {

	public static class PasswordReset extends Account.PasswordChange {

		public PasswordReset() {
		}

		public PasswordReset(final String token) {
			this.token = token;
		}

		public String token;

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

	private static final Form<PasswordReset> PASSWORD_RESET_FORM = form(PasswordReset.class);

	public static Result unverified() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		return ok(unverified.render());
	}

	private static final Form<GSNIdentity> FORGOT_PASSWORD_FORM = form(GSNIdentity.class);

	public static Result forgotPassword(final String email) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		Form<GSNIdentity> form = FORGOT_PASSWORD_FORM;
		if (email != null && !email.trim().isEmpty()) {
			form = FORGOT_PASSWORD_FORM.fill(new GSNIdentity(email));
		}
		return ok(password_forgot.render(form));
	}

	public static Result doForgotPassword() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<GSNIdentity> filledForm = FORGOT_PASSWORD_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not fill in his/her email
			return badRequest(password_forgot.render(filledForm));
		} else {
			// The email address given *BY AN UNKNWON PERSON* to the form - we
			// should find out if we actually have a user with this email
			// address and whether password login is enabled for him/her. Also
			// only send if the email address of the user has been verified.
			final String email = filledForm.get().email;

			// We don't want to expose whether a given email address is signed
			// up, so just say an email has been sent, even though it might not
			// be true - that's protecting our user privacy.
			flash(LocalAuthController.FLASH_MESSAGE_KEY,
					Messages.get(
							"playauthenticate.reset_password.message.instructions_sent",
							email));

			final User user = User.findByEmail(email);
			if (user != null) {
				// yep, we have a user with this email that is active - we do
				// not know if the user owning that account has requested this
				// reset, though.
				final GSNUsernamePasswordAuthProvider provider = GSNUsernamePasswordAuthProvider
						.getProvider();
				// User exists
				if (user.emailValidated) {
					provider.sendPasswordResetMailing(user, ctx());
					// In case you actually want to let (the unknown person)
					// know whether a user was found/an email was sent, use,
					// change the flash message
				} else {
					// We need to change the message here, otherwise the user
					// does not understand whats going on - we should not verify
					// with the password reset, as a "bad" user could then sign
					// up with a fake email via OAuth and get it verified by an
					// a unsuspecting user that clicks the link.
					flash(LocalAuthController.FLASH_MESSAGE_KEY,
							Messages.get("playauthenticate.reset_password.message.email_not_verified"));

					// You might want to re-send the verification email here...
					provider.sendVerifyEmailMailingAfterSignup(user, ctx());
				}
			}

			return redirect(routes.LocalAuthController.index());
		}
	}

	/**
	 * Returns a token object if valid, null if not
	 * 
	 * @param token
	 * @param type
	 * @return
	 */
	private static TokenAction tokenIsValid(final String token, final Type type) {
		TokenAction ret = null;
		if (token != null && !token.trim().isEmpty()) {
			final TokenAction ta = TokenAction.findByToken(token, type);
			if (ta != null && ta.isValid()) {
				ret = ta;
			}
		}

		return ret;
	}

	public static Result resetPassword(final String token) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final TokenAction ta = tokenIsValid(token, Type.PASSWORD_RESET);
		if (ta == null) {
			return badRequest(no_token_or_invalid.render());
		}

		return ok(password_reset.render(PASSWORD_RESET_FORM
				.fill(new PasswordReset(token))));
	}

	public static Result doResetPassword() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<PasswordReset> filledForm = PASSWORD_RESET_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			return badRequest(password_reset.render(filledForm));
		} else {
			final String token = filledForm.get().token;
			final String newPassword = filledForm.get().password;

			final TokenAction ta = tokenIsValid(token, Type.PASSWORD_RESET);
			if (ta == null) {
				return badRequest(no_token_or_invalid.render());
			}
			final User u = ta.targetUser;
			try {
				// Pass true for the second parameter if you want to
				// automatically create a password and the exception never to
				// happen
				u.resetPassword(new GSNUsernamePasswordAuthUser(newPassword),
						false);
			} catch (final RuntimeException re) {
				flash(LocalAuthController.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.reset_password.message.no_password_account"));
			}
			final boolean login = GSNUsernamePasswordAuthProvider.getProvider()
					.isLoginAfterPasswordReset();
			if (login) {
				// automatically log in
				flash(LocalAuthController.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.reset_password.message.success.auto_login"));

				return PlayAuthenticate.loginAndRedirect(ctx(),
						new GSNLoginUsernamePasswordAuthUser(u.email));
			} else {
				// send the user to the login page
				flash(LocalAuthController.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.reset_password.message.success.manual_login"));
			}
			return redirect(routes.LocalAuthController.login());
		}
	}

	public static Result oAuthDenied(final String getProviderKey) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		return ok(oAuthDenied.render(getProviderKey));
	}

	public static Result exists() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		return ok(exists.render());
	}

	public static Result verify(final String token) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final TokenAction ta = tokenIsValid(token, Type.EMAIL_VERIFICATION);
		if (ta == null) {
			return badRequest(no_token_or_invalid.render());
		}
		final String email = ta.targetUser.email;
		User.verify(ta.targetUser);
		flash(LocalAuthController.FLASH_MESSAGE_KEY,
				Messages.get("playauthenticate.verify_email.success", email));
		if (LocalAuthController.getLocalUser(session()) != null) {
			return redirect(routes.LocalAuthController.index());
		} else {
			return redirect(routes.LocalAuthController.login());
		}
	}
}
