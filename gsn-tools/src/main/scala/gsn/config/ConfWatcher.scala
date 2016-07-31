package gsn.config

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
        sender ! vsMap(sensorid)   
      //case GetSensorsConf =>
//        sender ! VsConfs(vsMap.map(_._2).toSeq)
    }

}

case class ModifiedVsConf(vs:VsConf)
case class DeletedVsConf(vs:VsConf)


case class GetSensorConf(sensorid:String)
case object GetSensorsConf