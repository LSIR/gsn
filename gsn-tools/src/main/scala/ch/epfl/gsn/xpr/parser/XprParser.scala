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
* File: src/ch/epfl/gsn/xpr/parser/XprParser.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.xpr.parser
import scala.util.parsing.combinator._
import ch.epfl.gsn.xpr._
import scala.util.Try

object XprParser extends JavaTokenParsers{
  def num: Parser[ValueXpr] = floatingPointNumber ^^ (a=>ValueXpr(a.toFloat))
  def vari: Parser[VarXpr] = this.ident ^^ (a=>VarXpr(a))
  def term: Parser[Xpr] = num | vari  
  import OpEnum._
  def op: Parser[Op] = (">="|"<="| "="|"<"|">")  ^^ (a=>OpEnum.withName(a))
  def biXpr: Parser[BinaryXpr] = ( term ~ op ~ term) ^^ {
    case  t1 ~ o ~ t2 => BinaryXpr(o,t1,t2)
  }
  def parseXpr(input: String) = Try{
    val p=parseAll(biXpr, input)
    if (p.successful) p.get
    else throw new Exception(p.toString)    
  }
} 