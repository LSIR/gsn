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
* File: src/ch/epfl/gsn/xpr/Xpr.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.xpr

import OpEnum._

trait Xpr {
  def toString:String
}

case class VarXpr(name:String) extends Xpr{
  override def toString=name
}
case class ValueXpr(value:Any) extends Xpr{
  override def toString= if (value==null) "null" else value.toString
}
case class BinaryXpr(operator:Op,x1:Xpr,x2:Xpr) extends Xpr{
  override def toString=x1.toString+" "+operator+" "+x2.toString
}
