package controllers.gsn.api

import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import scala.util.Try
import org.zeromq.ZMQ
import java.io.ByteArrayInputStream
import java.util.Date
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.{Input => kInput};
import gsn.beans.StreamElement;
import gsn.data._
import gsn.data.format._


object WebSocketForwarder extends Controller{
  
    val kryo = new Kryo()

    def socket(sensorid:String) = WebSocket.using[String] { request =>
        
        val context = ZMQ.context(1)
		    val subscriber = context.socket(ZMQ.SUB)
		    subscriber.connect("tcp://localhost:22022")
		    subscriber.setReceiveTimeOut(3000)
		    subscriber.subscribe((sensorid+":").getBytes)

        val in = {
            def cont: Iteratee[String, Unit] = Cont {
                case Input.EOF => {
                  subscriber.close()
                  Done((), Input.EOF)
                }
                case other => {
                  cont
                }
            }
            cont
        }

        val out = Enumerator.repeat {
            Try {
                var rec = subscriber.recv()
    				    while (rec == null){
    				        subscriber.subscribe((sensorid+":").getBytes)
    				        rec = subscriber.recv()
    				    }
    					  val bais = new ByteArrayInputStream(rec)
    					  bais.skip(sensorid.length + 2)
    					  val o = kryo.readObjectOrNull(new kInput(bais), classOf[StreamElement])
    					  val ts = new Date(o.getTimeStamp())
    					  "{ \"timestamp\":\""+ts+"\","+o.getFieldNames.map(x =>  "\"" + x.toLowerCase() + "\":" + o.getData(x)).mkString(",")+"}"
            }.recover{
              case t:Exception=>
                "{\"error\": \""+t.getMessage+"\"}" 
            }.get 
		    }
		
        (in, out)
    }
 
}