package gsn.process

import gsn.data._
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