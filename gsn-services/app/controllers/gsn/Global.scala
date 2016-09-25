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
* File: app/controllers/gsn/Global.scala
*
* @author Jean-Paul Calbimonte
* @author Julien Eberle
*
*/
package controllers.gsn

import com.typesafe.config.ConfigFactory

import akka.actor._
import ch.epfl.gsn.config.GsnConf
import ch.epfl.gsn.data.DataStore
import ch.epfl.gsn.data.SensorStore
import play.api._
import play.api.libs.concurrent.Akka
import play.mvc.Call
import models.gsn.auth.User
import models.gsn.auth.DataSource

import models.gsn.auth.SecurityRole;

import org.zeromq.ZMQ

import com.feth.play.module.pa.PlayAuthenticate
import com.feth.play.module.pa.PlayAuthenticate.Resolver
import com.feth.play.module.pa.exceptions.AccessDeniedException
import com.feth.play.module.pa.exceptions.AuthException


object Global extends GlobalSettings {
  private lazy val conf = ConfigFactory.load
  val gsnConf = GsnConf.load(conf.getString("gsn.config"))
  val ds = new DataStore(gsnConf)
  val pageLength = conf.getInt("gsn.ui.pagination.length")
  val context  = ZMQ.context(1)
  
  override def onStart(app: Application) {
    Logger.info("Application has started")
    Akka.system(app).actorOf(Props(new SensorStore(ds)),"gsnSensorStore")
    
    PlayAuthenticate.setResolver(new Resolver() {
            override def login: Call = {
                controllers.gsn.auth.routes.LocalAuthController.login
            }
            override def  afterAuth: Call = {
                // The user will be redirected to this page after authentication
                // if no original URL was saved
                controllers.gsn.auth.routes.LocalAuthController.index
            }

            override def  afterLogout: Call = {
                controllers.gsn.auth.routes.LocalAuthController.index
            }

            override def auth(provider: String): Call = {
                // You can provide your own authentication implementation,
                // however the default should be sufficient for most cases
                com.feth.play.module.pa.controllers.routes.Authenticate.authenticate(provider)
            }

            override def  onException(e: AuthException): Call = {
                e match {
                  case ad : AccessDeniedException => controllers.gsn.auth.routes.Signup.oAuthDenied(ad.getProviderKey())
                  case other => {super.onException(e)}
                }
               }

            override def  askLink: Call = {
                // We don't support moderated account linking in this sample.
                // See the play-authenticate-usage project for an example
               controllers.gsn.auth.routes.Account.askLink
            }

            override def  askMerge: Call = {
                // We don't support moderated account merging in this sample.
                // See the play-authenticate-usage project for an example
                controllers.gsn.auth.routes.Account.askMerge
            }
        })

		initialData

    }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  def initialData = {
		if (SecurityRole.find.findRowCount == 0) {
				val role = new SecurityRole()
				role.roleName = controllers.gsn.auth.LocalAuthController.USER_ROLE
				role.save
				val admin = new SecurityRole()
				admin.roleName = controllers.gsn.auth.LocalAuthController.ADMIN_ROLE
				admin.save
		}
	}
  
  def hasAccess(user: User,toWrite: Boolean,vsnames: String*):Boolean =  
    vsnames.foldRight[Boolean](true)((vs,b) => b && hasAccess(user,toWrite,vs))

       
   def hasAccess(user: User,toWrite: Boolean,vsname: String):Boolean = {
     val ds = DataSource.findByValue(vsname)
     ds == null || (ds.getIs_public && !toWrite) || user.hasAccessTo(ds, toWrite)
   }

  /*
  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.info("piropo******************************************"+ex.getMessage)
    //Future.successful{Ok("belamnto")}
    Future.successful{BadRequest("belamnto")}
    /*
    Future.successful(InternalServerError(
      views.html.error("tribo",ex)
    ))*/
  }*/
}
