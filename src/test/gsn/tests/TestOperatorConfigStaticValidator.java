package gsn.tests;

import org.testng.annotations.Test;
import org.testng.annotations.Test;
import static org.easymock.classextension.EasyMock.*;
import static org.testng.Assert.*;
import gsn.core.OperatorConfigStaticValidator;
import gsn.channels.DataChannel;
import gsn.channels.GSNChannel;
import gsn2.conf.OperatorConfig;
import gsn2.conf.ChannelConfig;

public class TestOperatorConfigStaticValidator {

  @Test
  public void testValidation(){
    OperatorConfigStaticValidator validator = new OperatorConfigStaticValidator();
    assertFalse(validator.proccess(new OperatorConfig()));
    assertFalse(validator.proccess(new OperatorConfig("name","a.b.c")));
    OperatorConfig config = new OperatorConfig("name", "gsn.operators.MirrorOperator");
    assertTrue(validator.proccess(config));
    config.setChannels(new ChannelConfig[]{});
    assertTrue(validator.proccess(config));
    config.setChannels(new ChannelConfig[]{new ChannelConfig("name1"),new ChannelConfig("name2")});
    assertTrue(validator.proccess(config));
    config.setChannels(new ChannelConfig[]{new ChannelConfig("name1"),new ChannelConfig("name1")});
    assertFalse(validator.proccess(config));
  }
}
