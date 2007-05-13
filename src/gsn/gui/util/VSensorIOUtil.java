package gsn.gui.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * A utility class for working with virtual sensor files
 * 
 * @author Mehdi Riahi
 */
public class VSensorIOUtil {
	private String virtualSensorDir;

	private String disabledDir;

	private FileFilter filter;

	private File vsDirFile;

	private File disabledDirFile;

	public VSensorIOUtil(String virtualSensorDir, String disabledDir) {
		this.virtualSensorDir = virtualSensorDir;
		this.disabledDir = disabledDir;

		vsDirFile = new File(virtualSensorDir);
		disabledDirFile = new File(disabledDir);

		filter = new FileFilter() {
			public boolean accept(File file) {
				if (!file.isDirectory() && file.getName().endsWith(".xml") && !file.getName().startsWith("."))
					return true;
				return false;
			}
		};

	}

	private void checkDirectories() throws IOException {
		if (vsDirFile.exists()) {
			if (!vsDirFile.isDirectory()) {
				System.out.println(vsDirFile + " should be a directory");
				throw new IOException(vsDirFile + " should be a directory");
			}
		} else {
			if (!vsDirFile.mkdir()) {
				System.out.println("Can not create virtual sensor directory : " + vsDirFile);
				throw new IOException("Can not create virtual sensor directory : " + vsDirFile);
			}
		}

		if (disabledDirFile.exists()) {
			if (!disabledDirFile.isDirectory()) {
				System.out.println(disabledDirFile + " should be a directory");
				throw new IOException(disabledDirFile + " should be a directory");
			}
		} else {
			if (!disabledDirFile.mkdir()) {
				System.out.println("Can not create virtual sensor directory : " + disabledDirFile);
				throw new IOException("Can not create virtual sensor directory : " + disabledDirFile);
			}
		}
	}

	public File[] readVirtualSensors() throws IOException {
		checkDirectories();
		File files[] = vsDirFile.listFiles(filter);
		return files;
	}

	public File[] readDisabledVirtualSensors() throws IOException {
		checkDirectories();
		File files[] = disabledDirFile.listFiles(filter);
		return files;
	}

	public void saveInVirtualSensors(File vsFile) {

	}

	public void saveInDesabledVirtualSensors(File vsFile) {

	}

	/**
	 * Moves the specified vsensor file to the disabled virtual sensors directory
	 * @param vsFile
	 * @return <code>true</code> if the specified vsensor file successfully moved
	 *         to the disabled virtual sensors directory
	 * @throws IOException
	 */
	public boolean disableVirtualSensor(File vsFile) throws IOException {
		File destFile = new File(disabledDirFile, vsFile.getName());
		System.out.println(destFile.getAbsolutePath());
		checkDirectories();
		if (destFile.exists())
			destFile.delete();
		return vsFile.renameTo(destFile);
	}

	/**
	 * Moves the the specified vsensor file to the enabled virtual sensors directory
	 * @param vsFile
	 * @return <code>true</code> if the specified vsensor file successfully moved
	 *         to the enabled virtual sensors directory
	 * @throws IOException
	 */
	public boolean enableVirtualSensor(File vsFile) throws IOException {
		File destFile = new File(vsDirFile, vsFile.getName());
		System.out.println(destFile.getAbsolutePath());
		checkDirectories();
		if (destFile.exists())
			destFile.delete();
		return vsFile.renameTo(destFile);
	}
}
