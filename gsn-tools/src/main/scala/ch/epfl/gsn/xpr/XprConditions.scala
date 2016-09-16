/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/xpr/XprConditions.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.xpr

import ch.epfl.gsn.xpr.parser.XprParser
import scala.util.Failure
import scala.util.Try

object XprConditions {
  def isCondition(binary:BinaryXpr)={
    binary match {
      case BinaryXpr(op,VarXpr(v),ValueXpr(value)) => true
      case _ => false
    }
  }
  def parseConditions(conditions:Array[String])=Try{
    conditions.map{c=>
      val parsed=XprParser.parseXpr(c)
      if (parsed.isSuccess && !isCondition(parsed.get))
        throw new IllegalArgumentException("Invalid condition "+parsed.get)
      parsed.get
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