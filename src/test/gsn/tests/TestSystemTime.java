package gsn.tests;

import org.testng.annotations.Test;
import static org.easymock.classextension.EasyMock.*;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import gsn2.wrappers.SystemTime;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.utils.Param;
import gsn.beans.StreamElement;

public class TestSystemTime {
    @Test
    public void testSytemTime() throws InterruptedException {
        DataChannel mock = createMock(DataChannel.class);
        SystemTime systemTime = new SystemTime(new WrapperConfig("gsn2.wrappers.SystemTime",new Param("clock-period","1")),mock);
        mock.write((StreamElement)anyObject());
        expectLastCall().atLeastOnce();
        replay(mock);
        systemTime.start();
        Thread.sleep(1000);
        systemTime.stop();
        systemTime.dispose();
        verify(mock);

    }
}
