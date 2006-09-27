package gsn.pid;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

public class PIDUtils {
	
	public static final String DIRECTORY_SERVICE_PID = "gsn-dir.pid";
	
	public static final String GSN_PID = "gsn.pid";
	
	public static void createPID(String pid_file) throws IOException,RuntimeException {
		
		String tempDirPath = System.getProperty("java.io.tmpdir");
		File tempFile = new File(tempDirPath+"/"+pid_file);
		if (tempFile.exists())
			throw new RuntimeException("The "+tempFile.getAbsolutePath()+ " file exists. Start failed.");
		tempFile.createNewFile();
		tempFile.deleteOnExit();
	}

	public static void killPID(String pid_file) {
		String tempDirPath = System.getProperty("java.io.tmpdir");
		File tempFile = new File(tempDirPath+"/"+pid_file);
		if (!tempFile.isFile())
			throw new RuntimeException("The "+tempFile.getAbsolutePath()+ " file doesn't exists. Stop failed.");
		tempFile.delete();
	}

	public static boolean isPIDExist(String pid_file) {
		String tempDirPath = System.getProperty("java.io.tmpdir");
		File tempFile = new File(tempDirPath+"/"+pid_file);
		return tempFile.isFile()&&tempFile.exists();
	}
}
