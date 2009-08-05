package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;

import java.io.File;
import java.io.Serializable;

import org.apache.log4j.Logger;

public class DiskSpaceWrapper implements Wrapper {

	private static final int            DEFAULT_SAMPLING_RATE       = 1000;

	private int                         samplingRate                = DEFAULT_SAMPLING_RATE;

	private final transient Logger      logger                      = Logger.getLogger(DiskSpaceWrapper.class);

	private transient DataField[]       outputStructureCache        = new DataField[]{new DataField("FREE_SPACE", "bigint", "Free Disk Space")};

	private File[] roots;

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private boolean isActive;

	public DiskSpaceWrapper(WrapperConfig conf, DataChannel channel) {
		logger.info("Initializing DiskSpaceWrapper Class");
		String javaVersion = System.getProperty("java.version");
		if(!javaVersion.startsWith("1.6"))
			throw new RuntimeException("Error in initializing DiskSpaceWrapper because of incompatible jdk version: " + javaVersion + " (should be 1.6.x)");
		this.conf = conf;
		this.dataChannel = channel;
		isActive=true;
	}

	public void dispose() {

	}

	public DataField[] getOutputFormat() {
		return outputStructureCache;
	}

	public void start() {
		while(isActive){
			try{
				Thread.sleep(samplingRate);
			}catch (InterruptedException e){
				logger.error(e.getMessage(), e);
			}
			roots = File.listRoots();
			long totalFreeSpace = 0;
			for (int i = 0; i < roots.length; i++) {
				totalFreeSpace += roots[i].getFreeSpace();
			}

			//convert to MB
			totalFreeSpace = totalFreeSpace / (1024 * 1024);
			StreamElement streamElement = new StreamElement(new String[]{"FREE_SPACE"}, new Byte[]{DataTypes.BIGINT}, new Serializable[] {totalFreeSpace
			},System.currentTimeMillis());
			dataChannel.write(streamElement);
		}
	}

	public void stop() {
		isActive=false;
	}

}
