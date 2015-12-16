package controllers.gsn

import com.typesafe.config.ConfigFactory

import akka.actor._
import gsn.config.GsnConf
import gsn.data.DataStore
import gsn.data.SensorStore
import gsn.security.SecurityData
import play.api._
import play.api.libs.concurrent.Akka
import play.mvc.Call

import models.gsn.auth.SecurityRole;

import com.feth.play.module.pa.PlayAuthenticate
import com.feth.play.module.pa.PlayAuthenticate.Resolver
import com.feth.play.module.pa.exceptions.AccessDeniedException
import com.feth.play.module.pa.exceptions.AuthException


object Global extends GlobalSettings {
  private lazy val conf=ConfigFactory.load
  val gsnConf=GsnConf.load(conf.getString("gsn.config"))
  val ds =new DataStore(gsnConf)  
  val acDs=new SecurityData(ds)
  
  val globalKey=conf.getString("gsn.security.globalKey")
  
  override def onStart(app: Application) {
    Logger.info("Application has started")
    val sec=new SecurityData(ds)
    //to enable 
    //sec.upgradeUsersTable    
    Akka.system(app).actorOf(Props(new SensorStore(ds)),"gsnSensorStore")
    
    PlayAuthenticate.setResolver(new Resolver() {
            override def login: Call = {
                controllers.gsn.auth.routes.LocalAuthController.index
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
