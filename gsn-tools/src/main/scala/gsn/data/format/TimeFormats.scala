package gsn.data.format

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import org.joda.time.format.DateTimeFormatter

object TimeFormats {
  def formatTime(t:Long)(implicit timeFormat:Option[String])=timeFormat match{
    case Some("unixTime") | None => t
    case _ => getTimeFormat(timeFormat).withZoneUTC().print(t)
  }

  def formatTime(t:Long, timeFormat:DateTimeFormatter)=
    timeFormat.print(t)

  private def getTimeFormat(tf:Option[String])={// e.g. yyyyMMdd    
    tf match {
      case Some("iso8601") => ISODateTimeFormat.dateTimeNoMillis()
      case Some("iso8601ms") => ISODateTimeFormat.dateTime()
      
      case Some(f) => 
        Try(DateTimeFormat.forPattern(f))              
        .recover{case e =>
          println("error here "+e)
          throw new IllegalArgumentException(s"Invalid time format: $f ${e.getMessage}")}.get 
      case None => ISODateTimeFormat.dateHourMinuteSecond()
    }
  }
}