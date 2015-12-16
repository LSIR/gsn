package gsn.data

import java.nio.charset.Charset
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory

trait DataSerializer{
  def ser(s:Sensor,props:Seq[String]):Object=
    ser(SensorData(Seq(),s),props,false)
  def ser(data:SensorData,props:Seq[String],latest:Boolean):Object
  def ser(data:Seq[SensorData],props:Seq[String]=Seq(),latest:Boolean=false):Object
  //def ser(data:Seq[Sensor],props:Seq[String]=Seq(),latest:Boolean=false):Object
  val dataLimit=ConfigFactory.load.getInt("gsn.data.limit")
}

  private def valueToJson(any:Any):JsValue=any match{       
    case d:Double=>JsNumber(d)
    case f:Float=>JsNumber(f)
    case i:Int=>JsNumber(i)
    case l:Long=>JsNumber(l)
    case s:String=>JsString(s)
    case a:Any=>JsString(a.toString) 
    case null=>JsNull
  }

