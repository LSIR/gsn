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
* File: src/ch/epfl/gsn/config/WatchServiceTask.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.config

import akka.actor.ActorRef
import java.nio.file.FileSystems
import collection.JavaConversions._
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds._
import java.io.File

class WatchServiceTask(notifyActor: ActorRef) extends Runnable {
    private val watchService = FileSystems.getDefault.newWatchService()

    def watch(path: Path) =
      path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    def run() {
      try {
        while (!Thread.currentThread.isInterrupted) {
          val key = watchService.take
          key.pollEvents foreach {event =>
		    val relativePath = event.context.asInstanceOf[Path]
		    val path = key.watchable.asInstanceOf[Path].resolve(relativePath)
		    event.kind match {
		      case ENTRY_CREATE =>
		        notifyActor ! Created(path.toFile)
		      case ENTRY_DELETE =>
		        notifyActor ! Deleted(path.toFile)
		      case ENTRY_MODIFY =>
		        notifyActor ! Modified(path.toFile)
		      case x =>
		        //logger.warn(s"Unknown event $x")
		    }
          }
          key reset
        }
      } catch {
          case e: InterruptedException =>
            //  logger.info("Interrupting, bye!")
        } finally {
            watchService close
        }
   }
}

sealed trait FileSystemChange
case class Created(fileOrDir: File) extends FileSystemChange
case class Deleted(fileOrDir: File) extends FileSystemChange
case class Modified(fileOrDir: File) extends FileSystemChange

case class MonitorDir(path: Path)