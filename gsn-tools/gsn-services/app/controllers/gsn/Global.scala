package controllers.gsn

import com.typesafe.config.ConfigFactory

import akka.actor._
import gsn.config.GsnConf
import gsn.data.DataStore
import gsn.data.SensorStore
import gsn.security.SecurityData
import play.api._
import play.api.libs.concurrent.Akka


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