package providers;

import play.data.Form
import java.util.HashMap
import java.util.Map
import java.util.Map.Entry
import java.util.UUID
import play.Application
import play.Logger
import play.data.Form
import play.data.validation.Constraints.Email
import play.data.validation.Constraints.MinLength
import play.data.validation.Constraints.Required
import play.mvc.Call
import play.mvc.Http.Context
import com.feth.play.module.mail.Mailer.Mail.Body
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser
import com.feth.play.module.pa.user.AuthUser
import com.google.inject.Inject
import models.gsn.{LocalUser,LocalUserManager}
import org.mindrot.jbcrypt.BCrypt
import models.gsn.UserManager

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider._


case class Login (@Required @Email email: String, @Required @MinLength(5) password: String) extends UsernamePasswordAuthProvider.UsernamePassword 

case class Signup(@Required @Email email: String, @Required @MinLength(5) password: String) extends UsernamePasswordAuthProvider.UsernamePassword 

class GSNUsernamePasswordAuthProvider @Inject() (app: Application)	extends	UsernamePasswordAuthProvider[String, LocalUser, LocalUser, Login, Signup](app)

object GSNUsernamePasswordAuthProvider {
  
	override def verifyWithToken(token: String) = {
	  LocalUserManager.selectWithFilter(("VERIFY",token)).headOption match {
	    case Some(x) => { 
	                      val u = LocalUser(x._pk,x.user,x.username, x.password,x.firstname,x.lastname,x.email,None)
	                      LocalUserManager.updateOrCreate(u)
	                      u
	                    }
	    case None    => null
	  }
	}

	override def generateVerificationRecord(u: LocalUser) = {
		val token = UUID.randomUUID().toString();
		LocalUserManager.updateOrCreate(LocalUser(u._pk,u.user,u.username, u.password,u.firstname,u.lastname,u.email,Some(token)))
    token
	}

	override def getVerifyEmailMailingSubject(user: LocalUser,ctx: Context) = "Please verify your email address"

	override def getVerifyEmailMailingBody(verificationRecord: String, user: LocalUser,ctx: Context)= new Body(verificationRecord)

	override def buildLoginAuthUser(login: Login,ctx: Context) = LocalUser(-1, -1, "", login.password,"","",login.email,None)

	override def transformAuthUser(signupUser: LocalUser,ctx: Context) = 
	  LocalUserManager.selectWithFilter(("USERNAME",signupUser.username)).head

	override def buildSignupAuthUser(signup: Signup, ctx: Context) = LocalUser(-1, -1, "", signup.password,"","",signup.email,None)

	override def loginUser(user: LocalUser) = {
	  LocalUserManager.selectWithFilter(("USERNAME",user.username)).headOption match {
	    case Some(x) => x.verify match {
	      case Some(_) => UsernamePasswordAuthProvider.LoginResult.USER_UNVERIFIED
	      case None    => if (BCrypt.checkpw(user.password, x.password)) 
	                          UsernamePasswordAuthProvider.LoginResult.USER_LOGGED_IN
	                      else
	                          UsernamePasswordAuthProvider.LoginResult.WRONG_PASSWORD
	    }
	    case None    => UsernamePasswordAuthProvider.LoginResult.NOT_FOUND
	  }
	}

	override def signupUser(user: LocalUser) = {
	  LocalUserManager.selectWithFilter(("USERNAME",user.username)).headOption match {
	    case Some(x) => x.verify match {
	      case Some(_) => UsernamePasswordAuthProvider.SignupResult.USER_EXISTS_UNVERIFIED
	      case None    => UsernamePasswordAuthProvider.SignupResult.USER_EXISTS
	    }
	    case None    => {
	                       LocalUserManager.updateOrCreate(user)
	                       UsernamePasswordAuthProvider.SignupResult.USER_CREATED_UNVERIFIED
	                    }
	  }
	}
	
	override def handleLogin(ctx: Context) =  UsernamePasswordAuthProvider.handleLogin(ctx)
	
	override def handleSignup(ctx: Context) =  UsernamePasswordAuthProvider.handleSignup(ctx)

  override def getSignupForm: Form[Signup] =  Form.form(classOf[Signup])
  
  override def getLoginForm: Form[Login] = Form.form(classOf[Login])
  
  override def userExists(authUser: UsernamePasswordAuthUser) = controllers.gsn.routes.Application.userExists()
  
  override def userUnverified(authUser: UsernamePasswordAuthUser) = controllers.gsn.routes.Application.userUnverified();

}
