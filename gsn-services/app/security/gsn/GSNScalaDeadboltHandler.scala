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
* File: app/security/gsn/GSNScalaDeadboltHandler.scala
*
* @author Julien Eberle
*
*/
package security.gsn

import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import play.api.mvc.{Request, Result, Results, Session}
import play.api.mvc.Controller
import be.objectify.deadbolt.core.models.Subject
import models.gsn.auth.User
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import play.mvc.Http
import collection.JavaConversions._
import com.feth.play.module.pa.PlayAuthenticate
import com.feth.play.module.pa.user.AuthUserIdentity
import play.core.j.JavaHelpers

class GSNScalaDeadboltHandler(dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler {

  def beforeAuthCheck[A](request: Request[A]) = {
   if (PlayAuthenticate.isLoggedIn(new Http.Session(request.session.data))) {
			// user is logged in
			None
		} else {
			// user is not logged in

			// call this if you want to redirect your visitor to the page that
			// was requested before sending him to the login page
			// if you don't call this, the user will get redirected to the page
			// defined by your resolver
		  val context = JavaHelpers.createJavaContext(request)
			val originalUrl = PlayAuthenticate.storeOriginalUrl(context)
			context.flash().put("error", "You need to log in first, to view '" + originalUrl + "'")
      Option(Future(play.mvc.Results.redirect(PlayAuthenticate.getResolver().login()).toScala().withSession(new Session(context.session().toMap))))

		} 
  }


  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    None
  }

  override def getSubject[A](request: Request[A]): Option[Subject] = {
    val context = JavaHelpers.createJavaContext(request)
    Option(User.findByAuthUserIdentity(PlayAuthenticate.getUser(context)))
  }

  def onAuthFailure[A](request: Request[A]): Future[Result] = {
    Future {Results.Forbidden("Forbidden")}
    // if the user has a cookie with a valid user and the local user has
		// been deactivated/deleted in between, it is possible that this gets
		// shown. You might want to consider to sign the user out in this case.

  }
}