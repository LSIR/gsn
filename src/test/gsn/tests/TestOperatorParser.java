package gsn.tests;

import org.testng.annotations.Test;
import static org.easymock.classextension.EasyMock.*;
import static org.testng.Assert.*;
import org.jibx.runtime.JiBXException;
import org.apache.commons.io.FileUtils;
import gsn.core.OperatorParser;
import gsn.core.OperatorConfig;
import gsn.utils.ChainOfReponsibility;

import java.io.File;
import java.io.IOException;

public class TestOperatorParser {

  String tempDir = System.getProperty("java.io.tmpdir");

  @Test
  public void testBadOp() throws JiBXException, IOException {
    ChainOfReponsibility<OperatorConfig> mockAddition = createMock(ChainOfReponsibility.class);

    replay(mockAddition);

    OperatorParser parser = new OperatorParser(mockAddition,mockAddition);
    String operator1 = "<op2erator name='test1'>"+
            "<processor>org.com</processor>"+
            "</operator>";
    File op = new File(tempDir + "/" + "op.xml");
    FileUtils.writeStringToFile(op,operator1);
    parser.fileAddition(op.getAbsolutePath());
    verify(mockAddition);
    op.delete();

  }

  @Test
  public void testMissingFile() throws JiBXException, IOException {
    ChainOfReponsibility<OperatorConfig> mockAddition = createMock(ChainOfReponsibility.class);
    replay(mockAddition);
    OperatorParser parser = new OperatorParser(mockAddition,mockAddition);
    File op1 = new File(tempDir + "/" + "op123.xml");
    parser.fileAddition(op1.getAbsolutePath());
    parser.fileRemoval(op1.getAbsolutePath());
    verify(mockAddition);
  }

  @Test
  public void testGoodOp() throws JiBXException, IOException {
    ChainOfReponsibility<OperatorConfig> mockAddition = createMock(ChainOfReponsibility.class);
    ChainOfReponsibility<OperatorConfig> mockRemoval = createMock(ChainOfReponsibility.class);
    OperatorParser parser = new OperatorParser(mockAddition,mockRemoval);

    OperatorConfig expectedOperator = new OperatorConfig("test1", "org.com");
    expect(mockAddition.proccess(expectedOperator)).andReturn(true);
    expect(mockAddition.proccess(expectedOperator)).andReturn(true);
    expect(mockRemoval.proccess(expectedOperator)).andReturn(true);

    replay(mockAddition);
    replay(mockRemoval);

    String operator = "<operator name='test1'>"+
            "<processor>org.com</processor>"+
            "</operator>";

    File op1 = new File(tempDir + "/" + "op1.xml");
    File op2 = new File(tempDir + "/" + "op2.xml");

    FileUtils.writeStringToFile(op1,operator);
    FileUtils.writeStringToFile(op2,operator);


    assertEquals(parser.createOperatorConfig(op2.getAbsolutePath()),expectedOperator);

    parser.fileAddition(op1.getAbsolutePath());
    parser.fileAddition(op2.getAbsolutePath());

    parser.fileChanged(op1.getAbsolutePath());
    parser.fileRemoval("abc");
    parser.fileRemoval(op2.getAbsolutePath());

    parser.fileChanged(op2.getAbsolutePath());

    verify(mockAddition);
    verify(mockRemoval);

    op1.delete();
    op2.delete();
  }

  @Test
  public void testFileChange() throws Exception{
    ChainOfReponsibility<OperatorConfig> mockAddition = createMock(ChainOfReponsibility.class);
    ChainOfReponsibility<OperatorConfig> mockRemoval = createMock(ChainOfReponsibility.class);
    OperatorParser parser = new OperatorParser(mockAddition,mockRemoval);

    OperatorConfig expectedOperator1 = new OperatorConfig("test1", "org.com");
    OperatorConfig expectedOperator2 = new OperatorConfig("test2", "org.com");

    expect(mockAddition.proccess(expectedOperator1)).andReturn(true);
    expect(mockRemoval.proccess(expectedOperator1)).andReturn(true);
    expect(mockAddition.proccess(expectedOperator2)).andReturn(true);

    replay(mockAddition);
    replay(mockRemoval);

    String content1 = "<operator name='test1'>"+
            "<processor>org.com</processor>"+
            "</operator>";

    String content2= "<operator name='test2'>"+
            "<processor>org.com</processor>"+
            "</operator>";

    File op = new File(tempDir + "/" + "op.xml");
    FileUtils.writeStringToFile(op, content1);
    parser.fileAddition(op.getAbsolutePath());
    FileUtils.writeStringToFile(op,content2);
    parser.fileChanged(op.getAbsolutePath());
  
    verify(mockAddition);
    verify(mockRemoval);

    op.delete();

  }
}
