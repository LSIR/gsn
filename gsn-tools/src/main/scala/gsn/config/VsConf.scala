package gsn.config

import xml._

case class VsConf(name:String,accessProtected:Boolean,priority:Int,timeZone:String,
    description:String,poolSize:Option[Int],address:Map[String,String],storage:Option[StorageConf],
    storageSize:Option[String],processing:ProcessingConf,streams:Seq[StreamConf]) {
  
}

//case class VsConfs(confs:Seq[VsConf])

object VsConf extends Conf{
  lazy val vs=defaults.getConfig("vs")
  val defaultPoolSize=vs.getInt("poolSize")
  val defaultPriority=vs.getInt("priority")
  val defaultProtected=vs.getBoolean("protected")
  val defaultOutputRate=vs.getInt("outputRate")
  val defaultUniqueTimestamps=vs.getBoolean("uniqueTimestamps")
  def create(xml:Elem)=VsConf(
		  (xml \@ "name").replaceAll(" ", ""),
		  attBool(xml,"protected",defaultProtected),
		  attInt(xml,"priority",defaultPriority),
		  att(xml,"time-zone",null),
		  (xml \ "description").text,
		  (xml \ "life-cycle").headOption.map(lc=>attInt(lc,"pool-size",defaultPoolSize)),
		  (xml \ "addressing" \ "predicate").map(p=>(p \@ "key",p.text)).toMap,
		  (xml \ "storage").headOption.flatMap{s=>
		    s.attribute("url").headOption.map(u=>StorageConf.create(s))},		  
		  (xml \ "storage").headOption.map(s=>s \@ "history-size"),
		  ProcessingConf.create((xml \ "processing-class").head) ,
		  (xml \ "streams" \ "stream").map(s=>StreamConf.create(s))		  
  )
  def load(path:String):VsConf=create(XML.load(path))
}

case class ProcessingConf(className:String,uniqueTimestamp:Boolean,initParams:Map[String,String],
    rate:Option[Int],output:Seq[FieldConf],webInput:Option[WebInputConf])
object ProcessingConf extends Conf{
  def create(xml:Node)=ProcessingConf(
      (xml \ "class-name").text,
      (xml \ "unique-timestamps").headOption.map(a=>a.text.toBoolean).
        getOrElse(VsConf.defaultUniqueTimestamps),
      (xml \ "init-params" \ "param").map(p=>(p \@ "name",p.text)).toMap,
      (xml \ "output-specification").headOption.map(o=>attInt(o,"rate",VsConf.defaultOutputRate )),
      (xml \ "output-structure" \ "field").map(f=>FieldConf.create(f)),
      (xml \ "web-input").headOption.map(wi=>WebInputConf.create(wi))
  )
}

case class FieldConf(name:String,dataType:String,description:String,unit:Option[String])
object FieldConf {
  def create(xml:Node)=FieldConf(
      xml \@ "name",
      xml \@ "type",
      xml.text,
      xml.attribute("unit").map(_.toString))        
}
   
case class WebInputConf(password:String,commands:Seq[WebInputCommand])
object WebInputConf{
  def create(xml:Node)=WebInputConf(
      xml \@ "password",
      (xml \ "command").map(c=>WebInputCommand(c \@ "name",
          (c \ "field").map(f=>FieldConf.create(f))))
   )  
}
case class WebInputCommand(name:String,params:Seq[FieldConf])

case class StreamConf(name:String,rate:Int,count:Int,query:String,sources:Seq[SourceConf])
object StreamConf extends Conf{
  def create(xml:Node)=StreamConf(
      xml \@ "name",
      attInt(xml,"rate",0),
      attInt(xml,"count",0),
      (xml \ "query").text,
      (xml \ "source").map(s=>SourceConf.create(s)))
}

case class SourceConf(alias:String,query:String,storageSize:Option[String],slide:Option[String],
    disconnectBufferSize:Option[Int],samplingRate:Option[Double],wrappers:Seq[WrapperConf])
object SourceConf{
  def create(xml:Node)=SourceConf(
      xml \@ "alias",(xml \ "query").text,
      xml.attribute("storage-size").map(_.toString),
      xml.attribute("slide").map(_.toString),
      xml.attribute("disconnected-buffer-size").map(_.toString.toInt),
      xml.attribute("sampling-rate").map(_.toString.toDouble),
      (xml \ "address").map(w=>WrapperConf.create(w))
  )
}
    
case class WrapperConf(wrapper:String,params:Map[String,String],output:Seq[FieldConf])
object WrapperConf{
  def create(xml:Node)=WrapperConf(
      xml \@ "wrapper",
      (xml \ "predicate").map(p=>(p \@ "key",p.text)).toMap,
      (xml \ "field").map(f=>FieldConf.create(f)))
}    

      