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
* File: src/ch/epfl/gsn/process/DataProcess.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.process

import ch.epfl.gsn.data._
import scala.collection.mutable.ArrayBuffer

trait DataProcess {
  val name:String
  def process(ts:Series):Series
}

class ExponentialSmoothing(alpha:Double) extends DataProcess{
  override val name="exp-smoothing"
  override def process(ts:Series)={
    val doubles=ts.asDoubles
    var count=0
    var current=0d
    val newSeries=doubles.map{d=>
      if (count==0) 
        current=d        
      else 
        current=alpha*d+(1-alpha)*current
      count+=1
      current
  	}
    
    Series(ts.output,newSeries.toSeq)        
  }
}

class SimpleMovingAverage(size:Int) extends DataProcess{
  override val name="simple-moving-avg"
  override def process(ts:Series)={
    var sma:Double=0
    val window=new ArrayBuffer[Double]()
    val doubles=ts.asDoubles
    val newSeries=doubles.map{d=>
      window+=d
      if (window.size==size){
        sma=window.sum/size
        window.remove(0)                
      }
      sma
    }
    Series(ts.output,newSeries.toSeq)
  }
}

class WeightedMovingAverage(size:Int) extends DataProcess{
  override val name="weighted-moving-avg"
  override def process(ts:Series)={
    var total= 0d
    var numer= 0d
    var boot=true
    val denom=size*(size+1)/2
    val window=new ArrayBuffer[Double]()
    val doubles=ts.asDoubles
    val newSeries=doubles.map{d=>
      var wma=0d
      window+=d
      if (window.size<=size) {
          numer=numer+d*window.size
          total=total+d
      }
      if (window.size>size){
          numer=numer+size*d-total
            total=total+d-window(0)
      
        window.remove(0)                
      }
      wma=numer/denom
      if (window.size<size) 0
      else wma
    }
    
    Series(ts.output,newSeries.toSeq)
  }
}
  