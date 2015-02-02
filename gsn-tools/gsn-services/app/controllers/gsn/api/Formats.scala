package controllers.gsn.api

case class OutputFormat(code:String) {
  val formats=Seq("csv","json")
  if  (!formats.exists(_==code))
    throw new IllegalArgumentException("Invalid format: "+code)
}
  
object Csv extends OutputFormat("csv")
object Json extends OutputFormat("json")	