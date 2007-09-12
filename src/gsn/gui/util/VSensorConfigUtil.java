package gsn.gui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import gsn.beans.VSensorConfig;

public class VSensorConfigUtil {

	public static HashMap<File, VSensorConfig> getVSensorConfigs(File[] files) {
		if (files == null)
			throw new RuntimeException("Null pointer Exception (" + (files == null) + ")");
		IBindingFactory bfact;
		IUnmarshallingContext uctx;
		try {
			bfact = BindingDirectory.getFactory(VSensorConfig.class);
			uctx = bfact.createUnmarshallingContext();
		} catch (JiBXException e1) {
			return null;
		}
		HashMap<File, VSensorConfig> fileToVSensorConfigMap = new HashMap<File, VSensorConfig>(files.length);
		for (int i = 0; i < files.length; i++) {
			fileToVSensorConfigMap.put(files[i], getVSensorConfig(uctx, files[i].getAbsolutePath()));
		}
		return fileToVSensorConfigMap;
	}

	private static VSensorConfig getVSensorConfig(IUnmarshallingContext uctx, String fileName) {
		VSensorConfig configuration;
		try {
			configuration = (VSensorConfig) uctx.unmarshalDocument(new FileInputStream(fileName), null);
			configuration.setFileName(fileName);
			if (!configuration.validate()) {
				// TODO: log messages
				// logger.error(new StringBuilder().append("Adding the virtual
				// sensor specified in ").append(fileName).append(
				// " failed because of one or more problems in configuration
				// file.").toString());
				// logger.error(new StringBuilder().append("Please check the
				// file and try again").toString());
			}
			return configuration;
		} catch (JiBXException e) {
			// TODO: log messages
			// logger.error(e.getMessage(), e);
			// logger.error(new StringBuilder().append("Adding the virtual
			// sensor specified in ").append(fileName).append(
			// " failed because there is syntax error in the configuration file.
			// Please check the configuration file and try again.")
			// .toString());
		} catch (FileNotFoundException e) {
			// TODO: log messages
			// logger.error(e.getMessage(), e);
			// logger.error(new StringBuilder().append("Adding the virtual
			// sensor specified in ").append(fileName).append(
			// " failed because the configuratio of I/O problems.").toString());
		}
		return null;
	}
	
	public static void saveVSensorConfig(VSensorConfig vSensorConfig, File file) throws FileNotFoundException, JiBXException{
		if(vSensorConfig == null)
			throw new RuntimeException("Null pointer Exception: VSensor config should not be null");
		IBindingFactory bfact;
		IMarshallingContext mctx;
		try {
			bfact = BindingDirectory.getFactory(VSensorConfig.class);
			mctx = bfact.createMarshallingContext();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			mctx.setIndent(4);
			mctx.marshalDocument(vSensorConfig, "UTF-8", null, fileOutputStream);
		} catch (JiBXException e) {
			throw e;
		} catch (FileNotFoundException e) {
			throw e;
		}
		
	}
}
