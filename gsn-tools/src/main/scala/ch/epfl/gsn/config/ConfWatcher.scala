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
* File: src/ch/epfl/gsn/config/ConfWatcher.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.config

import akka.actor.Actor
import akka.event.LoggingReceive
import com.typesafe.config.ConfigFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ConfWatcher extends Actor {
    val conf=ConfigFactory.load
    //val log = Logging(context.system, this)
    val watchServiceTask = new WatchServiceTask(self)
    val watchThread = new Thread(watchServiceTask, "WatchService")
    val vsDir=new File(conf getString "gsn.vslocation")    
    if (!vsDir.exists) 
      throw new IllegalStateException(s"Virtual sensors configuration missing: ${vsDir.getPath}")
    val vsMap=new collection.mutable.HashMap[String,VsConf]
    val vsFileMap=new collection.mutable.HashMap[String,String]
        
    private def addVsConfig(f:File)={
      if (f.getName.endsWith(".xml")){
        val vs=VsConf load f.getPath
           vsMap+= ((vs.name.toLowerCase,vs ))
	    vsFileMap += ((f.getName,vs.name.toLowerCase))
	    Some(vs)
      }	    
      else None
    }
    
    private def removeVsConfig(file:File)={
      if (vsFileMap.contains(file.getName)){
        val vsname=vsFileMap(file.getName)
        val vs=vsMap(vsname)
        vsMap remove vsname  
        vsFileMap remove file.getName
        Some(vs)
      }
      else None
    }
    
    override def preStart() {     
      println("prestart actor conf watch at " + this.self.path)
      watchServiceTask .watch(vsDir.toPath)
      vsDir.listFiles.foreach{f=>
        self ! Created(f)       
	  //  addVsConfig(f)
	  }
      watchThread setDaemon true
      watchThread start 
    }

    override def postStop() {
      watchThread.interrupt()
    }

    def receive = LoggingReceive {
      //case MonitorDir(path) =>
      //  watchServiceTask watch path
      case Created(file) =>
        val vs=addVsConfig(file)
        if (vs.isDefined)
          context .parent ! ModifiedVsConf(vs.get) 
            //e.g. forward or broadcast to other actors
      case Deleted(file) =>     
        val vs=removeVsConfig(file)
        if (vs.isDefined)
          context.parent ! DeletedVsConf(vs.get)
      case Modified(file) =>
        removeVsConfig(file)
        val vs=addVsConfig(file)
        if (vs.isDefined)
          context .parent ! ModifiedVsConf(vs.get)        
      case GetSensorConf(sensorid) =>
        sender ! vsMap(sensorid.toLowerCase)
      //case GetSensorsConf =>
//        sender ! VsConfs(vsMap.map(_._2).toSeq)
    }

}

case class ModifiedVsConf(vs:VsConf)
case class DeletedVsConf(vs:VsConf)


case class GetSensorConf(sensorid:String)
case object GetSensorsConf
