package ch.epfl.gsn.config

import org.scalatest._

import ch.epfl.gsn.config.VsConf;

class VsConfigTest extends FunSpec with Matchers {
    
  describe("gps vs config"){
    val vs=VsConf.load("src/test/resources/conf/vs/gps.xml")
    it("should read params"){
      vs.name shouldBe "GPSVS"
      vs.description should startWith ("Virtual sensor producing random")
      vs.address.size shouldBe 1
      vs.address("type") shouldBe "test-sensor"
      vs.poolSize shouldBe Some(10)
      vs.priority shouldBe 100      
      vs.storageSize shouldBe Some("1m")
      vs.storage shouldBe None
      vs.processing.className shouldBe "gsn.vsensor.BridgeVirtualSensor"
      vs.processing.uniqueTimestamp shouldBe true
      vs.processing.initParams.size shouldBe 0
      val coms=vs.processing.webInput.get.commands
      coms.size shouldBe 2
      coms(0).name shouldBe "ploppy"
      coms(0).params(1).name  shouldBe "plop2"
      coms(0).params(1).dataType   shouldBe "*checkbox:apple|orange|banana"
      coms(0).params(1).description shouldBe "two"
      vs.processing .output(1).name shouldBe "longitude"
      
      val str = vs.streams(0)
      vs.streams.size shouldBe 1
      str.name shouldBe "sensor1"
      str.query shouldBe "select * from source1"
      str.sources(0).alias shouldBe "source1"
      str.sources(0).wrappers(0).wrapper shouldBe "gps-test"
      str.sources(0).wrappers(0).params("rate") shouldBe "1000"
      
    }
  }
  
  describe("scriptlet vs config"){
    val vs=VsConf.load("src/test/resources/conf/vs/scriptlet.xml")
    it("should read params"){
      vs.name shouldBe "scriptletVS"
      vs.processing.className shouldBe "gsn.processor.ScriptletProcessor"
      vs.processing.initParams.size shouldBe 2
      val pars=vs.processing.initParams
      pars("persistant") should be ("true")
      println(pars("scriptlet"))
      pars("scriptlet").split("\n").length should be (7)
      
      val src=vs.streams.head.sources.head
      src.query should be ("select tapo as papo from wrapper")
    }
  }
}