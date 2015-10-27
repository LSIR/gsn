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
import controllers.routes

import com.feth.play.module.pa.PlayAuthenticate
import com.feth.play.module.pa.PlayAuthenticate.Resolver
import com.feth.play.module.pa.exceptions.AccessDeniedException
import com.feth.play.module.pa.exceptions.AuthException


object Global extends GlobalSettings {
  private lazy val conf=ConfigFactory.load
  val gsnConf=GsnConf.load(conf.getString("gsn.config"))
  val ds =new DataStore(gsnConf)  
  val acDs=new SecurityData(ds)
  
  override def onStart(app: Application) {
    Logger.info("Application has started")
    val sec=new SecurityData(ds)
    //to enable 
    //sec.upgradeUsersTable    
    Akka.system(app).actorOf(Props(new SensorStore(ds)),"gsnSensorStore")
    
    PlayAuthenticate.setResolver(new Resolver() {
            override def login: Call = {
                routes.Application.index
            }
            override def  afterAuth: Call = {
                // The user will be redirected to this page after authentication
                // if no original URL was saved
                routes.Application.index
            }

            override def  afterLogout: Call = {
                routes.Application.index
            }

            override def auth(provider: String): Call = {
                // You can provide your own authentication implementation,
                // however the default should be sufficient for most cases
                com.feth.play.module.pa.controllers.routes.Authenticate.authenticate(provider)
            }

            override def  onException(e: AuthException): Call = {
                e match {
                  case ad : AccessDeniedException => routes.Application.oAuthDenied(ad.getProviderKey())
                  case other => {super.onException(e)}
                }
               }

            override def  askLink: Call = {
                // We don't support moderated account linking in this sample.
                // See the play-authenticate-usage project for an example
               null
            }

            override def  askMerge: Call = {
                // We don't support moderated account merging in this sample.
                // See the play-authenticate-usage project for an example
                null
            }
        })
    }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
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