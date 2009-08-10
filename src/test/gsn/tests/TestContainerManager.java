package gsn.tests;

import org.testng.annotations.Test;
import static org.easymock.classextension.EasyMock.*;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.testng.Assert.*;
import org.picocontainer.MutablePicoContainer;
import gsn.core.ContainerManager;
import gsn.core.OpStateChangeListener;
import gsn.utils.Parameter;
import gsn2.wrappers.WrapperConfig;
import gsn.beans.Operator;
import gsn2.wrappers.Wrapper;
import gsn.core.OperatorConfig;
import gsn.channels.ChannelConfig;
import gsn2.conf.Parameters;

public class TestContainerManager {
    @Test
    public void testLoadingGoodOp(){
        OperatorConfig goodConfig = new OperatorConfig("id1","gsn.tests.MockOperator");
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        changeListener.opLoading((MutablePicoContainer) anyObject());
        changeListener.opUnLoading((MutablePicoContainer) anyObject())  ;
        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertTrue(cm.proccess(goodConfig));
        assertNotNull( cm.getOperators().get(goodConfig));
        MockOperator operator = (MockOperator) cm.getOperators().get(goodConfig).getComponent(Operator.class);
        replay(operator.getMock());
        operator.getMock().start();
        verify(operator.getMock());

        reset(operator.getMock());
        assertTrue(cm.getUnloader().proccess(goodConfig));
        replay(operator.getMock());
        operator.getMock().stop();
        operator.getMock().dispose();
        verify(operator.getMock());
        verify(changeListener);
    }

    @Test
    public void testNotLoadingBadOp1(){
        OperatorConfig badConf = new OperatorConfig("id1","gsn.tests.MockOperator");
        badConf.setParameters(new Parameter("constructor","fail"));
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertFalse(cm.proccess(badConf)); //vs loading fails due to exception from constructor.
        verify(changeListener);
        assertTrue(cm.getOperators().isEmpty());
    }

    @Test
    public void testNotLoadingBadOp2(){
        OperatorConfig badConf2 = new OperatorConfig("id1","gsn.tests.MockOperator");
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        badConf2.setChannels(new ChannelConfig("my-channel",new WrapperConfig("gsn.tests.MockWrapper")),
                new ChannelConfig("my-channel2",new WrapperConfig("gsn.tests.MockWrapper2")));
        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertFalse(cm.proccess(badConf2)); //fails because of exception in the wrapper's constructor.
        verify(changeListener);
    }

    @Test
    public void testNotLoadingDueToExceptionInStartOfOperator(){
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        OperatorConfig okConf = new OperatorConfig("id","gsn.tests.MockOperator");
        okConf.setParameters(new Parameter("start","fail"));

        changeListener.opLoading((MutablePicoContainer) anyObject());
        changeListener.opUnLoading((MutablePicoContainer) anyObject())  ;

        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertFalse(cm.proccess(okConf)); //fails because of exception in the start method of the operator.
        verify(changeListener);

        //checking to see if the disposed is called.
        Operator operator =MockOperator.getMock();
        replay(operator);
        operator.start();
        operator.dispose();
        verify(operator);

    }

    @Test
    public void testNotProperlyUnLoadingDueToExceptionInStopOfOperator(){
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        OperatorConfig okConf = new OperatorConfig("id","gsn.tests.MockOperator");
        okConf.setParameters(new Parameter("stop","fail"));
        changeListener.opLoading((MutablePicoContainer) anyObject());
        changeListener.opUnLoading((MutablePicoContainer) anyObject())  ;

        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertTrue(cm.proccess(okConf));


        Operator operator =MockOperator.getMock();
        // Till here everything is fine
        assertTrue(cm.getUnloader().proccess(okConf));
        replay(operator);
        operator.start();
        operator.stop();
        operator.dispose();
        verify(operator);
        verify(changeListener);
        assertTrue(cm.getOperators().isEmpty());
    }

    @Test
    public void testNotLoadingDueToExceptionInConstructorOfWrapper(){
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        OperatorConfig okConf = new OperatorConfig("id","gsn.tests.MockOperator");
        okConf.setChannels(new ChannelConfig("c1",new WrapperConfig("gsn.tests.MockWrapper",new Parameters(new Parameter("constructor","fail")))));
      
        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertFalse(cm.proccess(okConf)); //fails because of exception in the start method of the operator.
        verify(changeListener);

        //checking to see if the disposed is called.
        Operator operator =MockOperator.getMock();
        Wrapper wrapper = MockWrapper.getMock();
        replay(operator);
        verify(operator);
        replay(wrapper);
        verify(wrapper);
        assertTrue(cm.getOperators().isEmpty());
    }

    @Test
    public void testNotLoadingDueToExceptionInStartOfWrapper(){
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
        OperatorConfig okConf = new OperatorConfig("id","gsn.tests.MockOperator");
        okConf.setChannels(new ChannelConfig("c1",new WrapperConfig("gsn.tests.MockWrapper",new Parameters(new Parameter("start","fail")))));
        changeListener.opLoading((MutablePicoContainer) anyObject());
        changeListener.opUnLoading((MutablePicoContainer) anyObject())  ;

        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertFalse(cm.proccess(okConf)); //fails because of exception in the start method of the operator.
        verify(changeListener);

        //checking to see if the disposed is called.
        Operator operator =MockOperator.getMock();
        Wrapper wrapper = MockWrapper.getMock();
        replay(operator);
        operator.start();
        operator.stop();
        operator.dispose();
        verify(operator);
        replay(wrapper);
        wrapper.start();
        wrapper.dispose();
        verify(wrapper);
        assertTrue(cm.getOperators().isEmpty());
    }

    @Test
    public void testNotLoadingDueToExceptionInDisposeOfWrapper(){
        //todo
        OperatorConfig okConf = new OperatorConfig("id","gsn.tests.MockOperator");
        okConf.setChannels(new ChannelConfig("c1",new WrapperConfig("gsn.tests.MockWrapper",new Parameters(new Parameter("dispose","fail")))));
        theNormalShutdown(okConf);
    }

    public void theNormalShutdown(OperatorConfig okConf){
        OpStateChangeListener changeListener = createMock(OpStateChangeListener.class);
         changeListener.opLoading((MutablePicoContainer) anyObject());
        changeListener.opUnLoading((MutablePicoContainer) anyObject())  ;
        replay(changeListener);
        ContainerManager cm = new ContainerManager();
        cm.addOpStateChangeListener(changeListener);
        assertTrue(cm.proccess(okConf));
        assertTrue(cm.getUnloader().proccess(okConf));
        verify(changeListener);

        //checking to see if the disposed is called.
        Operator operator =MockOperator.getMock();
        Wrapper wrapper = MockWrapper.getMock();
        replay(operator);
        operator.start();
        operator.stop();
        operator.dispose();
        verify(operator);
        replay(wrapper);
        wrapper.start();
        wrapper.stop();
        wrapper.dispose();
        verify(wrapper);
        assertTrue(cm.getOperators().isEmpty());
    }
    @Test
    public void testNotLoadingDueToExceptionInStopOfWrapper(){
        OperatorConfig okConf = new OperatorConfig("id","gsn.tests.MockOperator");
        okConf.setChannels(new ChannelConfig("c1",new WrapperConfig("gsn.tests.MockWrapper",new Parameters(new Parameter("stop","fail")))));
        theNormalShutdown(okConf);
    }


}
