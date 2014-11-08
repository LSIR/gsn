package gsn.config

import scala.xml._
import com.typesafe.config.ConfigFactory
object Binder {
  
  lazy val defaults=ConfigFactory.load("gsn")
  
  def container(path:String)={
    def take(seq:NodeSeq)=seq.headOption.map(_.text)
    def takeBool(seq:NodeSeq)=seq.headOption.map(_.text.toBoolean)
    def takeInt(seq:NodeSeq)=seq.headOption.map(_.text.toInt)
      
    val xml=XML.load(path)
    
    val zmq=ZmqConf(takeBool(xml \ "zmq-enable").getOrElse(defaults.getBoolean("zmq.enabled")),
    takeInt(xml \ "zmqproxy").getOrElse(0),
    takeInt(xml \ "zmqmeta").getOrElse(2) 
)
/*
    val cont=new ContainerConfig(
    take(xml \ "name").getOrElse(DEFAULT_WEB_NAME),
    take(xml \ "author").getOrElse(DEFAULT_WEB_AUTHOR),
    take(xml \ "description").getOrElse(""),
    take(xml \ "email").getOrElse(DEFAULT_WEB_EMAIL),
    (xml \ "port").text.toInt,    
    take(xml \ "time-format").getOrElse("dd/MM/yyyy HH:mm:ss Z"),
    takeBool(xml \ "access-control").getOrElse(false),
    takeInt(xml \ "ssl-port").getOrElse(DEFAULT_SSL_PORT),
    null,null,null,null   );
        (xml \ "ssl-key-store-password").map(n=>cont.sslKeyStorePassword=n.text)
    (xml \ "ssl-key-password").map(n=>cont.sslKeyPassword=n.text)
    (xml \ "access-control").map(n=>cont.acEnabled=n.text.toBoolean)
*/
    //cont.gets.getStorage =new StorageConfig
    
    //cont
    
  }

}