package ch.epfl.gsn.data

import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.joda.time.Interval
import org.joda.time.MonthDay
import ch.epfl.gsn.data.time.Periods
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.Duration
class DatesTest  extends FunSpec with Matchers {
    
  describe("date intervals"){
    val intr="07-01/07-20"
    it ("should parse interval period"){
            
      val tep=Periods.addConditions(new DateTime(2009,10,4,0,0).getMillis, intr)
      println("pipopip "+tep)
    }
    
  }
  describe("time period"){
    val intr="PT1H7S"
    it ("should parse time period"){
      val d=Period.parse(intr)
      println("vamos "+d.toStandardSeconds().getSeconds())
    }
    
  }

}