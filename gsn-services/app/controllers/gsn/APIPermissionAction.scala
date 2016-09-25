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
* File: app/controllers/gsn/APIPermissionAction.scala
*
* @author Julien Eberle
*
*/
package controllers.gsn

import be.objectify.deadbolt.java.actions.DynamicAction
import play.api.mvc._
import scala.concurrent.{Future, ExecutionContext}
import security.gsn.GSNDeadboltHandler
import play.core.j.JavaHelpers
import scalaoauth2.provider.AuthInfoRequest
import models.gsn.auth.User
import models.gsn.auth.DataSource
import scalaoauth2.provider._
import play.mvc.Http
import collection.JavaConversions._
import com.feth.play.module.pa.PlayAuthenticate
import org.slf4j.LoggerFactory

case class APIPermissionAction(toWrite: Boolean, vsnames: String*)(implicit ctx: ExecutionContext) extends ActionFunction[Request, ({type L[A] = Request[A]})#L] with OAuth2Provider {
  
  private val log = LoggerFactory.getLogger(classOf[APIPermissionAction])
  
  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    if (PlayAuthenticate.isLoggedIn(new Http.Session(request.session.data))) {
      val u = User.findByAuthUserIdentity(PlayAuthenticate.getUser(JavaHelpers.createJavaContext(request)))
      if (Global.hasAccess(u,toWrite,vsnames:_*)) block(request)
      else Future(Forbidden("Logged in user has no access to these resources"))
    }else{
      authorize(new GSNDataHandler())({authInfo => {
        val u = User.findById(authInfo.user.id)
        if (Global.hasAccess(u,toWrite,vsnames:_*)) block(AuthInfoRequest(AuthInfo[User](u, authInfo.clientId, authInfo.scope, authInfo.redirectUri), request))
        else Future(Forbidden("Logged in user has no access to these resources"))
      }})(request, ctx)
    }
  }
  
}