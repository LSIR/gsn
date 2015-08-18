package gsn.data.time

import org.joda.time.MonthDay
import org.joda.time.DateTime

object Periods {
  def addConditions(startTime:Long,period:String)={
   
    val split=period.split("/")
    val m1=MonthDay.parse("--"+split(0))
    val m2=MonthDay.parse("--"+split(1))
    val stYear=new DateTime(startTime).year.get()
    val endYear=DateTime.now.getYear
    (stYear to endYear).map{year=>
      s"""(timed > ${new DateTime(year,m1.getMonthOfYear,m1.getDayOfMonth,0,0).getMillis}
      && timed < ${new DateTime(year,m2.getMonthOfYear,m2.getDayOfMonth,0,0).getMillis})"""
    }.mkString(" || ")
  }
  
}