package gsn.pid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PIDUtils {

    public static final String DIRECTORY_SERVICE_PID = "gsn-dir.pid";

    public static final String GSN_PID = "gsn.pid";

    public static void createPID(String pid_file) throws IOException,
	    RuntimeException {

	String tempDirPath = System.getProperty("java.io.tmpdir");
	File tempFile = new File(tempDirPath + "/" + pid_file);
	if (tempFile.exists())
	    throw new RuntimeException("The " + tempFile.getAbsolutePath()
		    + " file exists. Start failed.");
	tempFile.createNewFile();
	tempFile.deleteOnExit();
    }

    public static void killPID(String pid_file) {
	String tempDirPath = System.getProperty("java.io.tmpdir");
	File tempFile = new File(tempDirPath + "/" + pid_file);
	if (!tempFile.isFile())
	    throw new RuntimeException("The " + tempFile.getAbsolutePath()
		    + " file doesn't exists. Stop failed.");
	tempFile.delete();
    }

    public static boolean isPIDExist(String pid_file) {
	String tempDirPath = System.getProperty("java.io.tmpdir");
	File tempFile = new File(tempDirPath + "/" + pid_file);
	return tempFile.isFile() && tempFile.exists();
    }

    /**
         * The method returns the first character of the specified file.
         * 
         * @param file
         * @return -1 if file is completely empty.
         * @throws IOException
         *                 If can't read the file.
         */
    public static int getFirstByteFrom(String file) throws IOException {
	String path = System.getProperty("java.io.tmpdir");
	FileInputStream fis = new FileInputStream(path + "/" + file);
	int toReturn = -1;
	if (fis.available() > 0)
	    toReturn = fis.read();
	fis.close();
	return toReturn;
    }
}
