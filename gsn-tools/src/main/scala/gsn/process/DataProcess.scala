package gsn.process

import gsn.data._
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
  