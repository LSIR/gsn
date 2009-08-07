package gsn.tests;

import org.testng.annotations.Test;
import gsn.core.DirectoryMonitor;
import gsn.core.FilePresenceListener;

import java.io.File;
import java.io.IOException;
import static org.easymock.EasyMock.*;

public class TestDirectoryMonitor {
  private String tempDir = System.getProperty("java.io.tmpdir");
  @Test(expectedExceptions = RuntimeException.class)
  public void testDirectoryExistance(){
    DirectoryMonitor m = new DirectoryMonitor(tempDir+"1234",10);
  }

  @Test
  public void testStartStop() throws IOException, InterruptedException {
    DirectoryMonitor m = new DirectoryMonitor(tempDir,100);
    m.start();
    FilePresenceListener mockListener = createMock(FilePresenceListener.class);
    m.addListener(mockListener);
    String fileName1 = tempDir + "/" + "some-fileName1.xml";
    String fileName2 = tempDir + "/" + "some-fileName2.xml";
    mockListener.fileAddition(fileName1);
    mockListener.fileAddition(fileName2);
    mockListener.fileChanged(fileName2);
    mockListener.fileRemoval(fileName2);
    mockListener.fileRemoval(fileName1);
    replay(mockListener);
    File file1 = new File(fileName1);
    file1.createNewFile();
    File file2 = new File(fileName2);
    file2.createNewFile();
    Thread.sleep(150);
    file2.setLastModified(System.currentTimeMillis() + 100000);
    Thread.sleep(150);
    file2.delete();
    file1.delete();
    Thread.sleep(120);
    verify(mockListener);
    m.stop();
    m.removeListener(mockListener);
  }



}
