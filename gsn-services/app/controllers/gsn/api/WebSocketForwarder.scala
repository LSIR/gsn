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
* File: app/controllers/gsn/api/WebSocketForwarder.scala
*
* @author Julien Eberle
*
*/
package controllers.gsn.api

import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import scala.util.Try
import org.zeromq.ZMQ
import controllers.gsn.Global
import java.util.Date
import ch.epfl.gsn.beans.StreamElement
import ch.epfl.gsn.data._
import ch.epfl.gsn.data.format._


object WebSocketForwarder extends Controller{


    def socket(sensorid:String) = WebSocket.using[String] { request =>
      
        val deserializer = new StreamElementDeserializer()
        
		    val subscriber = Global.context.socket(ZMQ.SUB)
		    subscriber.connect("tcp://localhost:"+Global.gsnConf.zmqConf.proxyPort)
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
                val o = deserializer.deserialize(sensorid, rec)
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