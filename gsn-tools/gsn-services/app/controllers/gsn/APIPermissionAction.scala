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

case class APIPermissionAction(vsnames: String*)(implicit ctx: ExecutionContext) extends ActionFunction[Request, ({type L[A] = Request[A]})#L] with OAuth2Provider {
  
  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    if (PlayAuthenticate.isLoggedIn(new Http.Session(request.session.data))) {
      val u = User.findByAuthUserIdentity(PlayAuthenticate.getUser(JavaHelpers.createJavaContext(request)))
      if (hasAccess(u,vsnames:_*)) block(request)
      else Future(Forbidden("Logged in user has no access to these resources"))
    }else{
      authorize(new GSNDataHandler())({authInfo => block(AuthInfoRequest(authInfo, request))})(request, ctx)
    }
  }
    
   def hasAccess(user:User,vsnames: String*):Boolean = 
    (!Global.gsnConf.accessControl.enabled) || 
    vsnames.foldRight[Boolean](true)((vs,b) => b && hasAccess(user,vs))

       
   def hasAccess(user:User,vsname: String):Boolean = {
     val ds = DataSource.findByValue(vsname)
     ds == null || user.hasAccessTo(ds)
   }
    
    
  
}