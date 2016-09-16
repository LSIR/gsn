package ch.epfl.gsn.xpr.parser

import org.scalatest.Matchers

import ch.epfl.gsn.xpr.parser.XprParser;

import org.scalatest.FunSpec
import ch.epfl.gsn.xpr._
import util._

class XprParserTest extends FunSpec with Matchers {
    
  describe("parse expression"){
    it("should get parsed function"){
      val parsed=XprParser.parseXpr("   trala   <=    humid")
      parsed should not be (null)
      parsed match {
        case Success(BinaryXpr(op,VarXpr(t1),VarXpr(t2)))=>
          op shouldBe OpEnum.Leq
          t1 shouldBe "trala"
          t2 shouldBe "humid"
        case _ => fail("not desired")
          
      }
    }
  }
  
  describe("parse condition list"){
    it ("should parse list of conditions"){
      val ser=XprConditions.serializeConditions(Array("val=1","val2<3"))
      println(ser.get.mkString(","))
      ser.get.mkString(",") shouldBe("val = 1.0,val2 < 3.0")
    }
    it ("should fail to get condition"){
      val ser=XprConditions.serializeConditions(Array("val=val1","val2<3"))
      ser.isFailure shouldBe true
    }
  }

}