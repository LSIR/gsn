package gsn.config

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