package gsn.tests;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import gsn.core.OperatorConfigStaticValidator;
import gsn2.wrappers.WrapperConfig;
import gsn.core.OperatorConfig;
import gsn.channels.ChannelConfig;

public class TestOperatorConfigStaticValidator {

    @Test
    public void testValidation(){
        OperatorConfigStaticValidator validator = new OperatorConfigStaticValidator();
        assertFalse(validator.proccess(new OperatorConfig()));
        assertTrue(validator.proccess(new OperatorConfig("name","a.b.c")));
        OperatorConfig config = new OperatorConfig("name", "gsn.operators.MirrorOperator");
        assertTrue(validator.proccess(config));
        config.setChannels(new ChannelConfig[]{});
        assertTrue(validator.proccess(config));

        ChannelConfig[] channelConfigs = {new ChannelConfig("name1",new WrapperConfig("gsn.tests.MockWrapper")), new ChannelConfig("name2",new WrapperConfig("gsn.tests.MockWrapper"))};
        assertTrue(validator.proccess(config));
        config.setChannels(channelConfigs);

        channelConfigs = new ChannelConfig[]{new ChannelConfig("name1",new WrapperConfig("gsn.tests.MockWrapper")),
                new ChannelConfig("name1",new WrapperConfig("gsn.tests.MockWrapper"))};
        config.setChannels(channelConfigs);

        assertTrue(validator.proccess(config));
    }
}
