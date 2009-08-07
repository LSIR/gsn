package gsn.tests;

import org.testng.annotations.Test;
import org.testng.annotations.Test;
import static org.easymock.classextension.EasyMock.*;
import static org.testng.Assert.*;
import gsn.core.OperatorConfigStaticValidator;
import gsn.core.OperatorCompatibilityValidator;
import gsn.channels.DataChannel;
import gsn.channels.GSNChannel;
import gsn2.conf.OperatorConfig;
import gsn2.conf.ChannelConfig;

public class TestOperatorCompatibilityValidator {
  @Test
  public void duplicateName(){
    OperatorCompatibilityValidator validator = new OperatorCompatibilityValidator();
    OperatorConfig config = new OperatorConfig("dupNAmE", "some.class");
    assertTrue(validator.proccess(config));
    validator.vsLoading(config);
    assertFalse(validator.proccess(new OperatorConfig("dupName","some.class")));
    validator.vsUnLoading(config);
    assertTrue(validator.proccess(config));
  }
}
