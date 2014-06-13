package gsn.xpr

import gsn.xpr.parser.XprParser
import scala.util.Failure
import scala.util.Try

object XprConditions {
  def isCondition(binary:BinaryXpr)={
    binary match {
      case BinaryXpr(op,VarXpr(v),ValueXpr(value)) => true
      case _ => false
    }
  }
  def serializeConditions(conditions:Array[String])=Try{
    conditions.map{c=>
      val parsed=XprParser.parseXpr(c)
      if (parsed.isSuccess && !isCondition(parsed.get))
        throw new IllegalArgumentException("Invalid condition "+parsed.get)
      parsed.get.toString
    }
  }
}