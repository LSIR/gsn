package gsn.tests;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.classname.DefaultClassLoadingPicoContainer;
import gsn.core.OperatorCompatibilityValidator;
import gsn.core.OperatorConfig;

public class TestOperatorCompatibilityValidator {
    @Test
    public void duplicateName(){
        OperatorCompatibilityValidator validator = new OperatorCompatibilityValidator();
        OperatorConfig config = new OperatorConfig("dupNAmE", "some.class");
        OperatorConfig config2 = new OperatorConfig("dupName", "some.class");


        MutablePicoContainer pico = new DefaultClassLoadingPicoContainer();
        pico.addComponent(config);
        assertFalse(validator.proccess(config2));
        assertTrue(validator.proccess(config));

        pico = new DefaultClassLoadingPicoContainer();
        pico.addComponent(config2);
        validator.opLoading(pico);
        validator.opUnLoading(pico);
        assertTrue(validator.proccess(config2));
    }
}
