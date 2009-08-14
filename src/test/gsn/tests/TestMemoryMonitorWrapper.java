package gsn.tests;

import org.testng.annotations.Test;
import gsn.utils.Param;
import gsn.channels.DataChannel;
import gsn.beans.StreamElement;

import static org.testng.Assert.*;
import static org.easymock.EasyMock.*;
import gsn2.wrappers.WrapperConfig;
import gsn2.wrappers.MemoryMonitor;
import gsn2.conf.Parameters;

@Test(sequential = true)
public class TestMemoryMonitorWrapper {

    @Test
    public void testMemoryMonitorWrapper() {
        WrapperConfig config = new WrapperConfig("gsn2.wrappers.MemoryMonitor",new Parameters(new Param("sampling-rate","abc")));
        DataChannel mockChannel = createMock(DataChannel.class);
        MemoryMonitor mm = new MemoryMonitor(config,mockChannel);
        assertEquals(mm.getOutputRate(),MemoryMonitor.DEFAULT_SAMPLING_RATE);
        replay(mockChannel);
        verify(mockChannel);

    }
    @Test
    public void testStreaming() throws InterruptedException {
        WrapperConfig config = new WrapperConfig("gsn2.wrappers.MemoryMonitor",new Parameters(new Param("sampling-rate","1")));
        DataChannel mockChannel = createMock(DataChannel.class);
        MemoryMonitor mm = new MemoryMonitor(config,mockChannel);
        assertEquals(mm.getOutputRate(),1);
        verify(mockChannel);
        reset(mockChannel);
        mockChannel.write((StreamElement) anyObject());
        expectLastCall().times(10);
        replay(mockChannel);
        mm.start();
        Thread.sleep(500);

        verify(mockChannel);
        mm.stop();
        reset(mockChannel);
        Thread.yield();
        Thread.sleep(500);
        verify(mockChannel);

        reset(mockChannel);
        mockChannel.write((StreamElement) anyObject());
        expectLastCall().atLeastOnce();
        mm.start();
        replay(mockChannel);
        Thread.sleep(500);
        verify(mockChannel);

    }
}
