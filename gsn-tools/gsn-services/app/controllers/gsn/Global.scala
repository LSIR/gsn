package controllers.gsn

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future


object Global extends GlobalSettings {

  override def onStart(app: Application) {
    println("cabilas************************************************************")
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.info("piropo******************************************"+ex.getMessage)
    println("dibabas")
    Future.successful{Ok("belamnto")}
    /*
    Future.successful(InternalServerError(
      views.html.error("tribo",ex)
    ))*/
  }
}