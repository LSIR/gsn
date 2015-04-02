package controllers.gsn.api

case class OutputFormat(code:String) {
  val formats=Seq("csv","json","xml")
  if  (!formats.exists(_==code))
    throw new IllegalArgumentException(s"Invalid format: $code")
}
  
object Csv extends OutputFormat("csv")
object Json extends OutputFormat("json")	
object Xml extends OutputFormat("xml")	