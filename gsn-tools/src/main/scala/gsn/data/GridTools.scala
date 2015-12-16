package gsn.data

import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import collection.JavaConversions._

object GridTools {
  type DoubleGrid=Array[Array[java.lang.Double]]
  case class BoundingBox(minX:Int,minY:Int,maxX:Int,maxY:Int)
  
  object BoundingBox{
    def apply(box:Seq[Int])=new BoundingBox(box(0),box(1),box(2),box(3))
  }

  def deserialize(bytes:Array[Byte])={
    val bis = new ByteArrayInputStream(bytes)
    val in = new ObjectInputStream(bis)    
    val deserial = in.readObject.asInstanceOf[DoubleGrid]
    in.close
    bis.close
    deserial
  }
    
  def crop(grid:DoubleGrid,box:BoundingBox)={
    val countY=box.maxY-box.minY+1
    val countX=box.maxX-box.minX+1
    
    val newGrid=grid.drop(box.minY).take(countY).map{row=>
      row.drop(box.minX).take(countX)
    }
    newGrid
  }
  
  private def computeAgg(seq:Seq[Double],op:String)= op match{
    case "max" => seq.max
    case "min" => seq.min
    case "sum" => seq.sum
    case "avg" => seq.sum/seq.size
    case _ => throw new IllegalArgumentException("Invalid aggregator: "+op)
  }
  
  def aggregate(grids:Seq[DoubleGrid],op:String,noValue:Double)={
    val maxX=grids.head.head.size
    val maxY=grids.head.size
    val newGrid=Array.ofDim[Double](maxY,maxX)
    
    (0 until maxY).map{i=>
      (0 until maxX).map{j=>
        val seq=grids.map{grid=>
          grid(i)(j).toDouble  
        }.filterNot(d=>d==noValue)
        if (seq.isEmpty) noValue
        else computeAgg(seq,op)
      }.toArray
    }.toArray            
  }  
  
  def summarize(grid:DoubleGrid,op:String,noValue:Double)={
    val flat=grid.flatten.filterNot(_ == noValue).map(_.toDouble)
    computeAgg(flat,op)    
  }
  
}