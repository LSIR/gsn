package gsn.xpr.parser

import org.scalatest.Matchers
import org.scalatest.FunSpec

class XprParserTest extends FunSpec with Matchers {
    
  describe("parse expression"){
    it("should get parsed function"){
      val parsed=XprParser.parse("4")
      parsed should not be (null)
    }
  }

}