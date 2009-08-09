package gsn.tests;

import org.testng.annotations.Test;
import org.testng.annotations.Test;
import static org.easymock.classextension.EasyMock.*;
import static org.testng.Assert.*;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.classname.DefaultClassLoadingPicoContainer;
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
        OperatorConfig config2 = new OperatorConfig("dupName", "some.class");


        MutablePicoContainer pico = new DefaultClassLoadingPicoContainer();
        pico.addComponent(config);
        assertTrue(validator.proccess(config));
        validator.opLoading(pico);
        assertFalse(validator.proccess(config2));

        pico = new DefaultClassLoadingPicoContainer();
        pico.addComponent(config2);
        validator.opUnLoading(pico);
        assertTrue(validator.proccess(config2));
    }
}
