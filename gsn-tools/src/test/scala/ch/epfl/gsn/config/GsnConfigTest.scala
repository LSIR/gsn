package ch.epfl.gsn.config

import org.scalatest._

import ch.epfl.gsn.config.GsnConf;

class GsnConfigTest extends FunSpec with Matchers {
    
  describe("default gsn config"){
    val gsn=GsnConf.defaultGsn 
    it("should get default parameters"){
      gsn.monitorPort shouldBe 22001 
      gsn.zmqConf.enabled shouldBe false
      gsn.storageConf.user should be("sa")
      gsn.slidingConf shouldBe None
    }
  }

  describe("custom gsn config"){
    val gsn=GsnConf.load("src/test/resources/conf/gsn_test.xml") 
    it("should get default parameters"){
      gsn.monitorPort shouldBe 22006
      gsn.zmqConf.enabled shouldBe false
      gsn.storageConf.user should be("sata")
      gsn.slidingConf shouldBe a [Some[_]]
      gsn.slidingConf.get.user shouldBe "pipo"
    }
  }

  
}