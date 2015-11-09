package controllers.gsn

import scalaoauth2.provider._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import models.gsn.auth.{User, Client}
import com.feth.play.module.pa.PlayAuthenticate
import play.mvc.Http
import play.core.j.JavaHelpers
import collection.JavaConversions._
import scala.concurrent.Future
import views.html._
import play.mvc.Http.Context
import play.api.data._
import play.api.data.Forms._
import controllers.gsn.auth.Forms._
import java.util.UUID

object OAuth2Controller extends Controller with OAuth2Provider {
  
    def accessToken = Action.async { implicit request =>
        issueAccessToken[AnyContent,User](new GSNDataHandler())
  }
  
    def auth = Action.async { implicit request => Future {
      Context.current.set(JavaHelpers.createJavaContext(request))
      if (PlayAuthenticate.isLoggedIn(new Http.Session(request.session.data))) {
       val u = User.findByAuthUserIdentity(PlayAuthenticate.getUser(JavaHelpers.createJavaContext(request)))
       val r = request.queryString.get("response_type")
       if (r.map(x => x.contains("code")).getOrElse(false)){
           try{
               val c = request.queryString.get("client_id").get.head
               val uri =request.queryString.get("redirect_uri").get.head
               Ok(access.auth.render(c, uri, u))
               
           } catch {
             case e: Throwable => BadRequest(e.getMessage)
           }
       }else{
         NotImplemented("Response type not implemented: "+ r.getOrElse("null"))
       }
      }else{
        Redirect(controllers.gsn.auth.routes.LocalAuthController.login())
      }
    }}
    
   def doAuth = Action.async { implicit request => Future {
     Context.current.set(JavaHelpers.createJavaContext(request))
      if (PlayAuthenticate.isLoggedIn(new Http.Session(request.session.data))) {
       val u = User.findByAuthUserIdentity(PlayAuthenticate.getUser(JavaHelpers.createJavaContext(request)))
       clientForm.bindFromRequest.fold(
           formWithErrors => {
               BadRequest("bad request")
            },
            clientData => {
            val c = new Client()
            c.name = clientData.client_id
            c.redirect = clientData.redirect_uri
            c.clientId = UUID.randomUUID().toString
            c.secret = UUID.randomUUID().toString
            c.code = UUID.randomUUID().toString
            c.user = u
            c.save()
            Redirect(clientData.redirect_uri+"?code="+c.code)
            }
       )
      }else{
        Redirect(controllers.gsn.auth.routes.LocalAuthController.login())
      }
    }}
  
}