package controllers.gsn.api

import play.api.mvc._
import com.typesafe.config.ConfigFactory
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatterBuilder
import collection.JavaConversions._
import controllers.gsn.Global

trait GsnService {
  lazy val conf=ConfigFactory.load

  //val validFormats=Seq(Csv,Json)
  val defaultFormat=Json

  val dateFormatter={
    val parsers=conf.getStringList("gsn.api.dateTimeFormats").map{d=>
      DateTimeFormat.forPattern(d).getParser
    }.toArray
    new DateTimeFormatterBuilder().append(null,parsers).toFormatter
  }

  def queryparam(name:String)(implicit req:Request[AnyContent])=
    req.queryString.get(name).map(_.head)
  

  def param[T](name:String,fun: String=>T,default:T)(implicit req:Request[AnyContent])=
    queryparam(name).map(fun(_)).getOrElse(default)

}