package controllers.gsn

import scalaoauth2.provider._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import models.gsn.LocalUser


object OAuth2Controller extends Controller with OAuth2Provider {
  def accessToken = Action.async { implicit request =>
    issueAccessToken[AnyContent,LocalUser](new GSNDataHandler())
  }
  
}