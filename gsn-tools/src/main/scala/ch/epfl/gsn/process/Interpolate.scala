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
* File: src/ch/epfl/gsn/process/Interpolate.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.process

import ch.epfl.gsn.data._
import scala.collection.mutable.ArrayBuffer

trait Interpolation extends DataProcess {
  //def process(ts:TimeSeries):TimeSeries
  
}

class LinearInterpolation(from:Long,rate:Long) extends Interpolation {
  override val name="linear-interpolation"
  override def process(s:Series)=s match{
    case ts:TimeSeries => process(ts)
    case _ => throw new IllegalArgumentException("Interpolation works only on Time series")
  }
  
  def process(ts:TimeSeries):TimeSeries={
    var currentTime=from
    var (lastData,lastTime):(Double,Long)=(0,-1)
    val datas=new ArrayBuffer[Any]()
    val times=new ArrayBuffer[Long]()
    ts.iterator.foreach{case (d,t)=>
      if (lastTime != -1){
        val slope=(d-lastData)/(t-lastTime)
        while (currentTime<=t){
          val y2=slope*(currentTime-lastTime)+lastData
          datas+= y2
          times+= currentTime
          currentTime=currentTime+rate
        }
      }
      lastData=d
      lastTime=t
    }
    new TimeSeries(ts.output,datas,times)    
  }
}