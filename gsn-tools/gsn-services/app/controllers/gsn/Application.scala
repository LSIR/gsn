package controllers.gsn

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt
import scala.util.Try
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatterBuilder
import com.typesafe.config.ConfigFactory
import akka.actor._
import gsn.data._
import gsn.xpr.XprConditions
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.http.HeaderNames
import play.Logger
import scala.util.Success
import play.data.Form
import providers._
import play.mvc.Http
import models.gsn.LocalUser
import com.feth.play.module.pa.PlayAuthenticate

object Application extends Controller{   
  
  
  def header = List((CACHE_CONTROL, "no-cache, no-store, must-revalidate"),  // HTTP 1.1
		                  (PRAGMA, "no-cache"),  // HTTP 1.0.
		                  (EXPIRES, "0"))  // Proxies.
  

  def index = Action.async {implicit request=>
    Future(Ok(views.html.index.render()))
  }
  
  def login = Action.async {implicit request=>
    Future(Ok(views.html.login.render(Form.form(classOf[Login]).bindFromRequest())))
  }
  
  def doLogin = Action.async {implicit request=> 
		  val filledForm = Form.form(classOf[Login]).bindFromRequest()
		  if (filledForm.hasErrors()) {
			// User did not fill everything properly
			  Future(BadRequest(views.html.login.render(filledForm)).withHeaders(header:_*))
		  } else {
			// Everything was filled
			  Future(GSNUsernamePasswordAuthProvider.handleLogin(Http.Context.current()).toScala.withHeaders(header:_*))
		  }
	}

  def signup() = Action.async {implicit request=>
      Future(Ok(views.html.signup.render(Form.form(classOf[Signup]).bindFromRequest()))
	  }

	def doSignup() = Action.async {implicit request=>
     val filledForm = Form.form(classOf[Signup]).bindFromRequest()
		 if (filledForm.hasErrors()) {
			// User did not fill everything properly
			Future(BadRequest(views.html.signup.render(filledForm)).withHeaders(header:_*))
		} else {
			// Everything was filled
		  Future(GSNUsernamePasswordAuthProvider.handleSignup(Http.Context.current()).toScala.withHeaders(header:_*))
		}
	}

	def userExists() = Action.async {implicit request=> Future(BadRequest("User exists."))}

	def userUnverified() = Action.async {implicit request=> Future(BadRequest("User not yet verified."))

	def verify(token: String) = Action.async {implicit request=> Future{
		val loginUser = upAuthProvider().verifyWithToken(token);
		if (loginUser == null) {
			NotFound
		}
		PlayAuthenticate.loginAndRedirect(Http.Context.current(), loginUser).toScala
	}
	}

	def oAuthDenied(providerKey: String) {
		Flash(FLASH_ERROR_KEY, "You need to accept the OAuth connection"
				+ " in order to use this website!")
		Redirect(routes.Application.index)
	}

	def upAuthProvider = Play.application().plugin(classOf[GSNUsernamePasswordAuthProvider])
	

}
