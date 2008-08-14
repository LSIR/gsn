package gsn.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import gsn.RailsRunner;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.httpclient.methods.multipart.Part;
import org.junit.Test;

public class StreamElementTest {

  @Test
  public void testRestToAndFromBehaviors() throws IOException {
    FileInputStream fis = new FileInputStream("webapp/img/button_cancel.png");
    byte[] binary = new byte[fis.available()];
    fis.read(binary);
    fis.close();
    
    String testString = "ABCDEFGHIJKLMNOPQSTUVWXYZ!@#$%^&*()+_)(*&^%$#@!~}{|'\":?><";
    StreamElement se = new StreamElement(
        new String[] {"field_1","field_2","field_3","field_4","field_5","field_6","field_7"},
        new Byte[] {DataTypes.BIGINT,DataTypes.TINYINT,DataTypes.INTEGER,DataTypes.DOUBLE,DataTypes.CHAR,DataTypes.VARCHAR,DataTypes.BINARY},
        new Serializable[] {123456789392873l,123,1234567,1234.12345,"A",testString,binary},123456789l);
    Part[] toRest = se.toREST();
    assertEquals( se.getFieldNames().length+1,toRest.length);
    ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
    toRest[6].send(testOutput);
    assertTrue(new String(testOutput.toByteArray()).indexOf(testString) >0);
    testOutput.reset();
    toRest[7].send(testOutput);
    assertTrue(testOutput.toString().indexOf(new String(binary))>0);
//    String rubyScript ="require 'rubygems';require 'mongrel';";
//    org.jruby.Main.main(new String[] {"-e",rubyScript});
    RailsRunner rails = new RailsRunner();
    rails.start();
//    RailsRunner.main(null);
    System.out.println("YES");
      
//    rails.stop();
  }
  
}
