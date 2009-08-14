package gsn.tests;

import gsn.beans.DataField;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;
import gsn2.wrappers.Wrapper;
import gsn.operators.OperatorInitializationException;
import static org.easymock.EasyMock.*;

public class MockWrapper implements Wrapper {

    private static  Wrapper mock ;

    private final WrapperConfig conf;

    private final DataChannel dataChannel;

    private DataField[] outputStructure = new DataField[0];

    private String canStart;
    private String canStop;
    private String canDispose;
    private String constructor;


    public MockWrapper(WrapperConfig config, DataChannel channel) {
        this.conf = config;
        this.dataChannel= channel;
        mock= createMock(Wrapper.class);
        canStart = conf.getParameters().getValue("start");
        canDispose = conf.getParameters().getValue("dispose");
        constructor = conf.getParameters().getValue("constructor");
        mock = (Wrapper) createMock(Wrapper.class);
        if (constructor!=null)
            throw new OperatorInitializationException("constructor-fail");
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


    public void dispose() {
        mock.dispose();
        if (canDispose!=null)
            throw new RuntimeException("Fail-Dispose");
    }



    public DataField[] getOutputFormat() {
        return outputStructure;
    }

    public static Wrapper getMock() {
        return mock;
    }

    public WrapperConfig getConf() {
        return conf;
    }

    public DataChannel getDataChannel() {
        return dataChannel;
    }

    public void setOutputStructure(DataField[] outputStructure) {
        this.outputStructure = outputStructure;
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

        retValue = "MockWrapper ( "
                + super.toString() + TAB
                + "conf = " + this.conf + TAB
                + "dataChannel = " + this.dataChannel + TAB
                + " )";

        return retValue;
    }

}
