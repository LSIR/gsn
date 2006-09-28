package gsn.pid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.Test;

public class PIDUtilsTesting {

    @Test
    public void creationOfAPIDFile() throws IOException {
	PIDUtils.createPID(PIDUtils.DIRECTORY_SERVICE_PID);
	assertTrue(PIDUtils.isPIDExist(PIDUtils.DIRECTORY_SERVICE_PID));
    }

    @Test(expected = RuntimeException.class)
    public void doubleCreationOfAPIDFile() throws IOException {
	PIDUtils.createPID(PIDUtils.DIRECTORY_SERVICE_PID);
	assertTrue(PIDUtils.isPIDExist(PIDUtils.DIRECTORY_SERVICE_PID));
	PIDUtils.createPID(PIDUtils.DIRECTORY_SERVICE_PID);
    }

    @After
    public void killPIDsAfter() throws IOException {
	PIDUtils.killPID(PIDUtils.DIRECTORY_SERVICE_PID);
	assertFalse(PIDUtils.isPIDExist(PIDUtils.DIRECTORY_SERVICE_PID));
    }

    public static junit.framework.Test suite() {
	return new JUnit4TestAdapter(PIDUtilsTesting.class);
    }

}
