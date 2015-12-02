package controllers.gsn.api

import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import scala.util.Try
import org.zeromq.ZMQ
import java.io.ByteArrayInputStream
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.{Input => kInput};
import gsn.beans.StreamElement;


object WebSocketForwarder extends Controller{
  
    val kryo = new Kryo()

    def socket(sensorid:String) = WebSocket.using[String] { request =>
        
        val context = ZMQ.context(1)
		    val subscriber = context.socket(ZMQ.SUB)
		    subscriber.connect("tcp://localhost:22022/")
		    subscriber.setReceiveTimeOut(3000)
		    subscriber.subscribe(sensorid.getBytes)

        val in = {
            def cont: Iteratee[String, Unit] = Cont {
                case Input.EOF => {
                  subscriber.close()
                  Done((), Input.EOF)
                }
                case _ => cont
            }
            cont
        }

        val out = Enumerator.repeat {
        
            Try {
                var rec = subscriber.recv(0)
    				    while (rec == null){
    				        subscriber.subscribe(sensorid.getBytes)
    				        rec = subscriber.recv(0)
    				    }
    					  val bais = new ByteArrayInputStream(rec)
    					  bais.skip(sensorid.length + 1)
    					  val o = kryo.readObjectOrNull(new kInput(bais), classOf[StreamElement])
    					  o.getFieldNames.toString()
    
    				    /*else{
    					      if (!subscriber){
    						        subscriber.disconnect(remoteContactPoint_DATA);
    						        connected = subscriber.base().connect(remoteContactPoint_DATA);
    					      }
    					      subscriber.subscribe(vsensor.getBytes());
    				    }*/
            }.recover{
              case t=>
                t.getMessage 
            }.get 
		    }
		
        (in, out)
    }
 
}