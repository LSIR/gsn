package ch.epfl.gsn.process

import org.scalatest.Matchers

import ch.epfl.gsn.process.ExponentialSmoothing;
import ch.epfl.gsn.process.LinearInterpolation;
import ch.epfl.gsn.process.SimpleMovingAverage;
import ch.epfl.gsn.process.WeightedMovingAverage;

import org.scalatest.FunSpec
import ch.epfl.gsn.data._

class DataProcessingTest extends FunSpec with Matchers {
    
  describe("time series"){
    val ts=Series(Output("f1","s1",null,null),Seq(1d,2d,3d,4d))
    it("should be smoothed"){
      val smooth=new ExponentialSmoothing(0.4)
      val serie=smooth.process(ts).series
      serie should be(Seq(1.0,1.4,2.04,2.824))
    }
    it("should be sma smoothed"){
      val smooth=new SimpleMovingAverage(3)
      val serie=smooth.process(ts).series
      serie should be(Seq(0,0,2,3))
    }
    it("should be wma smoothed"){
      val smooth=new WeightedMovingAverage(3)
      val serie=smooth.process(ts).series
      println(serie)
      serie should be(Seq(0.0,0.0,14d/6,20d/6))
    }
  }
  describe("time series irregular"){
    val ts=new TimeSeries(Output("f1","s1",null,null),
        Seq(1d,2d,3d,4d),Seq(1,3,7,13))
    it("should be interpolated"){
      val inter=new LinearInterpolation(1,2)
      val res=inter.process(ts)
      println(res)
    }
  }
}