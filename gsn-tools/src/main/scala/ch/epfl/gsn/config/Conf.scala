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
* File: src/ch/epfl/gsn/config/Conf.scala
*
* @author Jean-Paul Calbimonte
* @author Julien Eberle
*
*/
package ch.epfl.gsn.config

import scala.xml._
import com.typesafe.config.ConfigFactory

trait Conf{
  val defaults=ConfigFactory.load.getConfig("gsn")
  def take(seq:NodeSeq)=seq.headOption.map(_.text)
  def takeBool(seq:NodeSeq)=seq.headOption.map(_.text.toBoolean)
  def takeInt(seq:NodeSeq)=seq.headOption.map(_.text.toInt)
  def attBool(e:Elem,att:String,default:Boolean)=
     e.attribute(att).headOption.map(_.toString.toBoolean).getOrElse(default)
  def attInt(e:Node,att:String,default:Int)=
     e.attribute(att).headOption.map(_.toString.toInt).getOrElse(default)
  def att(e:Elem,att:String,default:String)=
     e.attribute(att).headOption.map(_.toString).getOrElse(default)
  
  
  lazy val zmq=defaults.getConfig("zmq")
  lazy val defaultZmq=ZmqConf(
    zmq.getBoolean("enabled"),zmq.getInt("proxyPort"),zmq.getInt("metaPort"))

  lazy val storage=defaults.getConfig("storage")
  lazy val defaultStorage=StorageConf(storage.getString("driver"),storage.getString("url"),
      storage.getString("user"),storage.getString("password"),None)

  lazy val defaultGsn=GsnConf(defaults.getInt("monitorPort"),defaults.getString("timeFormat"),
      defaultZmq,defaultStorage,None)
}
