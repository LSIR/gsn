package controllers.gsn

import scalaoauth2.provider._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import models.gsn.auth.{User, Client, OAuthCode}
import com.feth.play.module.pa.PlayAuthenticate
import be.objectify.deadbolt.scala.DeadboltActions
import security.gsn.GSNScalaDeadboltHandler
import play.mvc.Http
import play.core.j.JavaHelpers
import collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import views.html._
import controllers.gsn.auth.LocalAuthController
import play.mvc.Http.Context
import play.api.data._
import play.api.data.Forms._
import controllers.gsn.auth.Forms._
import java.util.UUID

object OAuth2Controller extends Controller with OAuth2Provider with DeadboltActions {
  
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
               val s = request.queryString.get("client_secret").get.head
               val client = Client.findById(c)
               if (client != null && client.secret == s){
                   Ok(access.auth.render(c, s, client.redirect, u))
               }else{
                   Forbidden("This client is not registered for accessing GSN, please contact the administrator if you need to add it.")
               }
           } catch {
             case e: Throwable => BadRequest(e.getMessage)
           }
       }else{
         NotImplemented("Response type not implemented: "+ r.getOrElse("null"))
       }
      }else{
        request.session.+(("pa.url.orig", request.uri))
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
               val client = Client.findById(clientData.client_id)
               if (client != null && client.secret == clientData.client_secret){
                   val c = OAuthCode.generate(u,client)
                   c.save()
                   Redirect(client.redirect+"?code="+c.code+"&response_type=code")
               }else{
                   Forbidden("This client is not registered for accessing GSN, please contact the administrator if you need to add it.")
               }
            }
       )
      }else{
        request.session.+(("pa.url.orig", request.uri))
        Redirect(controllers.gsn.auth.routes.LocalAuthController.login())
      }
    }}
   
   def listClients = Restrict(Array(LocalAuthController.USER_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
       Context.current.set(JavaHelpers.createJavaContext(request))  
       Ok(access.clientlist.render(Client.find.all().asScala, editClientForm))
    }}}
   
   def editClient = Restrict(Array(LocalAuthController.USER_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
     Context.current.set(JavaHelpers.createJavaContext(request))
       editClientForm.bindFromRequest.fold(
           formWithErrors => {
               Ok(access.clientlist.render(Client.find.all().asScala, formWithErrors))
            },
            clientData => { clientData.action match {
              case "add" => {
                                val c = new Client()
                                c.clientId = clientData.client_id
                                c.name = clientData.name
                                c.redirect = clientData.redirect
                                c.secret = clientData.client_secret
                                c.save()
                            }
              case "edit" => {
                                val c = Client.findById(clientData.client_id)
                                if (c == null) NotFound
                                c.clientId = clientData.client_id
                                c.name = clientData.name
                                c.redirect = clientData.redirect
                                c.secret = clientData.client_secret
                                c.save()
                             }
              case "del" => {
                                val c = Client.findById(clientData.client_id)
                                if (c == null) NotFound
                                c.delete()
                            }
               }
            })
            Ok(access.clientlist.render(Client.find.all().asScala, editClientForm))  
    }}}
 
}