package gsn.tests;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn.operators.OperatorInitializationException;
import gsn.core.OperatorConfig;
import static org.easymock.EasyMock.*;

import java.util.List;

public class MockOperator implements Operator {
    private OperatorConfig conf;
    private DataChannel outputChannel;
    private static Operator mock ;
    private String canStart;
    private String canStop;
    private String canDispose;
    private String constructor;

    public MockOperator(OperatorConfig conf,DataChannel outputChannel ) {
        this.outputChannel = outputChannel;
        this.conf = conf;
        canStart = conf.getParameters().getValue("start");
        canStop = conf.getParameters().getValue("stop");
        canDispose = conf.getParameters().getValue("dispose");
        constructor = conf.getParameters().getValue("constructor");
        mock = (Operator) createMock(Operator.class);
        if (constructor!=null)
            throw new OperatorInitializationException("constructor-fail");
        
    }

    public void dispose() {
        mock.dispose();
        if (canDispose!=null)
            throw new RuntimeException("Fail-Dispose");
    }

    public void process(String name, List<StreamElement> window) {
        mock.process(name,window);
    }

    public DataField[] getStructure() {
        return mock.getStructure();
    }

    public void start() {
        mock.start();
        if (canStart!=null)
            throw new RuntimeException("Fail-Start");
    }

    public void stop() {
        mock.stop();
        if (canStop!=null)
            throw new RuntimeException("Fail-Stop");
    }

    public static Operator getMock() {
        return mock;
    }

    public OperatorConfig getConf() {
        return conf;
    }

    public DataChannel getOutputChannel() {
        return outputChannel;
    }

    /**
     * Constructs a <code>String</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation
     * of this object.
     */
    public String toString()
    {
        final String TAB = "    ";

        String retValue = "";

        retValue = "MockOperator ( "
                + super.toString() + TAB
                + "conf = " + this.conf + TAB
                + "outputChannel = " + this.outputChannel + TAB
                + " )";

        return retValue;
    }

}
