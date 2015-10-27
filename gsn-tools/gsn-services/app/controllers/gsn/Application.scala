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
import play.Logger
import scala.util.Success

object Application extends Controller{   
  

  def index = Action.async {implicit request=>
    //request.body.
    Future(Ok(""))
    
  }

def oAuthDenied(provider: String) = Action.async {implicit request=>
    //request.body.
    Future(Ok("provider"))
    
  }
}
