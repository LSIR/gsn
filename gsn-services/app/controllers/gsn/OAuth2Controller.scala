package controllers.gsn

import scalaoauth2.provider._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.utils.UriEncoding
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
        Redirect(controllers.gsn.auth.routes.LocalAuthController.login()).withSession(request.session.+(("pa.url.orig", request.uri)))
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
                   Redirect(client.redirect+"?code="+c.code+"&response_type=code&user_name="+UriEncoding.encodePathSegment(u.name,"UTF-8")+"&user_email="+UriEncoding.encodePathSegment(u.email,"UTF-8"))
               }else{
                   Forbidden("This client is not registered for accessing GSN, please contact the administrator if you need to add it.")
               }
            }
       )
      }else{
        Redirect(controllers.gsn.auth.routes.LocalAuthController.login()).withSession(request.session.+(("pa.url.orig", request.uri)))
      }
    }}
   
   def listClients = Restrict(Array(LocalAuthController.USER_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
       Context.current.set(JavaHelpers.createJavaContext(request))
       val u = User.findByAuthUserIdentity(PlayAuthenticate.getUser(JavaHelpers.createJavaContext(request)))
       if (u.roles.filter(p => p.getName.equals(LocalAuthController.ADMIN_ROLE)).isEmpty()) {
           Ok(access.clientlist.render(Client.find.all().asScala, editClientForm))
       } else {
           Ok(access.clientlist.render(Client.findByUser(u).asScala, editClientForm))
       }
    }}}
   
   def editClient = Restrict(Array(LocalAuthController.USER_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
       Context.current.set(JavaHelpers.createJavaContext(request))
       val u = User.findByAuthUserIdentity(PlayAuthenticate.getUser(JavaHelpers.createJavaContext(request)))
       var ret:Result = null
       editClientForm.bindFromRequest.fold(
           formWithErrors => {
               println(formWithErrors)
               ret = BadRequest(access.clientlist.render(Client.find.all().asScala, formWithErrors))
            },
            clientData => { 
              clientData.action match {
              case "add" => {
                                val c = new Client()
                                c.clientId = clientData.client_id
                                c.name = clientData.name
                                c.redirect = clientData.redirect
                                c.secret = clientData.client_secret
                                c.user = u
                                println(clientData)
                                c.linked = clientData.linked
                                c.save()
                            }
              case "edit" => {
                                val c = Client.find.byId(clientData.id)
                                if (c == null){ 
                                  ret = NotFound
                                } else {
                                  c.setClientId(clientData.client_id)
                                  c.setName(clientData.name)
                                  c.setRedirect(clientData.redirect)
                                  c.setSecret(clientData.client_secret)
                                  c.setLinked(clientData.linked)
                                  c.update()
                                }
                             }
              case "del" => {
                                val c = Client.find.byId(clientData.id)
                                if (c == null) ret = NotFound
                                else c.delete()
                            }
               }
            })
            if (ret != null)  ret else Ok(access.clientlist.render(Client.find.all().asScala, editClientForm))  
    }}}
 
}