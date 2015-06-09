package gsn.data.discovery.util

object DataFormatter {

  /**
   * Remove the first and last character of the provided String: it is needed for the parsing of JSON files
   * which return quotes with text
   */
  def removeQuotes(str:String):String = {
    if (str != null) str.replaceAll("\"","") else ""
  }
  
  /**
   * Extract the fragment id of the provided URI
   * eg: http://google.ch/index#hello returns hello
   */
  def extractFragmentId(uri:String):String = {
    val s = uri.split("#")
    if (s.size >= 2) s(1) else uri
  }
}