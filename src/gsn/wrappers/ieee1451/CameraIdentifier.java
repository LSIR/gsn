package gsn.wrappers.ieee1451;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.InputStream;
import gsn.beans.StreamElement;
import gsn.vsensor.Container;
import gsn.wrappers.AbstractStreamProducer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * @author Surender Reddy (yerva, surenderreddy.yerva-at-epfl.ch)<br>
 */

public class CameraIdentifier extends AbstractStreamProducer {
    private ArrayList<String> camIPs = new ArrayList<String>();

    private ArrayList<Object[][][]> camTEDS = new ArrayList<Object[][][]>();

    private ArrayList<String> activeCams = new ArrayList<String>();

    private int RATE = 15000;

    private int TIMEOUT = 20000;

    private TedsToVSResult tedsResult;

    private final Logger logger = Logger.getLogger(CameraIdentifier.class);

    private int threadCounter = 0;

    private String status = NONE_ACTION;

    private TedsToVirtualSensor tedsToVirtualSensor;

    private static final String NONE_ACTION = "none";

    private static final String ADD_ACTION = "added";

    private static final String REMOVE_ACTION = "removed";

    private static final String ID_OUTPUT_FIELD = "ID";

    private static final String TEDS_OUTPUT_FIELD = "TEDS";

    private static final String STATUS_OUTPUT_FIELD = "STATUS";

    private static final String VSFILE_OUTPUT_FIELD = "VSFILE";

    private static final String[] OUTPUT_FIELD_NAMES = new String[] {
	    ID_OUTPUT_FIELD, TEDS_OUTPUT_FIELD, STATUS_OUTPUT_FIELD,
	    VSFILE_OUTPUT_FIELD };

    private static final Integer[] OUTPUT_FIELD_TYPES = new Integer[] {
	    DataTypes.VARCHAR, DataTypes.VARCHAR, DataTypes.VARCHAR,
	    DataTypes.VARCHAR };

    public boolean initialize(TreeMap context) {
	boolean toReturn = super.initialize(context);

	String pingCommand = "ping ";
	String pingCommandParams = null;
	if (System.getProperty("os.name").equals("Mac OS X"))
	    pingCommandParams = " -c1 -t1 ";
	else if (System.getProperty("os.name").toLowerCase().indexOf("linux") > 0)
	    pingCommandParams = " -c1 -w1 ";
	else
	    System.out.println("Not defined for your OS");
	camIPs.add(0, pingCommand + pingCommandParams + "192.168.51.30");
	camIPs.add(1, pingCommand + pingCommandParams + "192.168.51.31");
	camIPs.add(2, pingCommand + pingCommandParams + "192.168.51.32");
	camIPs.add(3, pingCommand + pingCommandParams + "192.168.51.33");
	camIPs.add(4, pingCommand + pingCommandParams + "192.168.51.34");
	// camIPs.add(5, pingCommand + pingCommandParams + "192.168.51.35");
	camTEDS.add(0, tedsCam1);
	camTEDS.add(1, tedsCam2);
	camTEDS.add(2, tedsCam3);
	camTEDS.add(3, tedsCam4);
	camTEDS.add(4, tedsCam5);
	camTEDS.add(5, tedsCam6);

	AddressBean addressBean = (AddressBean) context
		.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);
	if (addressBean.getPredicateValue("TIMEOUT") != null) {
	    TIMEOUT = Integer.parseInt((String) addressBean
		    .getPredicateValue("TIMEOUT"));
	}
	if (addressBean.getPredicateValue("RATE") != null) {
	    RATE = Integer.parseInt((String) addressBean
		    .getPredicateValue("RATE"));
	}
	// ------INITIALIZING THE TEMPLATE DIRECTORY ---------
	String templateDirPath = addressBean
		.getPredicateValue("templates-directory");
	if (templateDirPath == null) {
	    logger
		    .warn("The CameraIdentifier couldn't initialize. The >templates-directory< parameter is missing from the set of the wrapper configuration parameters.");
	    return false;
	}
	String templateFile = addressBean.getPredicateValue("template-file");
	if (templateFile == null) {
	    logger
		    .warn("The CameraIdentifier couldn't initialize. The >template-file< parameter is missing from the set of the wrapper configuration parameters.");
	    return false;
	}

	File templateFolder = new File(templateDirPath);
	if (!templateFolder.exists() || !templateFolder.isDirectory()
		|| !templateFolder.canRead()) {
	    logger
		    .warn("The CameraIdentifier couldn't initialize. Can't read >"
			    + templateFolder.getAbsolutePath() + "<.");
	    return false;
	}

	File templateF = new File(templateFolder.getAbsolutePath() + "/"
		+ templateFile + ".st");
	if (!templateF.exists() || !templateF.isFile() || !templateF.canRead()) {
	    logger
		    .warn("The CameraIdentifier couldn't initialize. Can't read >"
			    + templateF.getAbsolutePath() + "<.");
	    return false;
	}
	tedsToVirtualSensor = new TedsToVirtualSensor(templateDirPath,
		templateFile);
	// ------INITIALIZING THE TEMPLATE DIRECTORY ---------DONE

	setName("CameraIdentifier-Thread" + (++threadCounter));
	try {
	    getStorageManager().createTable(getDBAlias(),
		    getProducedStreamStructure());
	} catch (SQLException e) {
	    logger.error(e.getMessage(), e);
	    return false;
	}
	try {
	    Thread.sleep(4000);
	} catch (InterruptedException e) {
	    e.printStackTrace(); // To change body of catch statement use
	    // File | Settings | File Templates.
	}
	this.start();
	return toReturn;
    }

    public void run() {
	/**
         * Initial delay to make sure than non of packets are dropped b/c of the
         * intiaial delay.
         */
	try {
	    Thread.sleep(InputStream.INITIAL_DELAY_5000MSC * 2);
	} catch (InterruptedException e) {
	    e.printStackTrace(); // To change body of catch statement use
	    // File | Settings | File Templates.
	}
	while (isActive()) {
	    try {
		Thread.sleep(RATE);
	    } catch (InterruptedException e) {
		logger.error(e.getMessage(), e);
	    }
	    Boolean pingResult;
	    if (listeners.isEmpty())
		continue;
	    for (String strIP : camIPs) {
		try {
		    Process p = Runtime.getRuntime().exec(strIP);
		    try {
			p.waitFor();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		    int res = p.exitValue();
		    pingResult = (res == 0) ? true : false;
		    // System.out.println("PingResult:"+ (pingResult ?
		    // "true" :
		    // "false") + host);

		} catch (IOException e) {
		    pingResult = false;
		    // System.out.println("PingResult: false-"+host);
		}
		if (pingResult) {
		    if (activeCams.contains(strIP)) {
			// System.out.println("True: File not added");
		    } else {
			status = ADD_ACTION;
			generateStreamElement(new TEDS(camTEDS.get(camIPs
				.indexOf(strIP))), status);
			activeCams.add(strIP);
			// System.out.println("True: File added");
		    }
		} else {
		    if (activeCams.contains(strIP)) {
			activeCams.remove(strIP);
			status = REMOVE_ACTION;
			generateStreamElement(new TEDS(camTEDS.get(camIPs
				.indexOf(strIP))), status);
			boolean success = (new File(
				TedsToVirtualSensor.TARGET_VS_DIR
					+ tedsResult.fileName)).delete();
			if (!success) {
			    logger.warn("Can't remove the non-live camera.");
			}
		    }

		}
	    }
	}
    }

    private void generateStreamElement(TEDS teds, String status) {
	try {
	    if (status == ADD_ACTION)
		tedsResult = tedsToVirtualSensor.GenerateVS(teds);
	    if (status == REMOVE_ACTION)
		tedsResult = tedsToVirtualSensor.getTedsToVSResult(teds);
	    StreamElement streamElement = new StreamElement(
		    OUTPUT_FIELD_NAMES,
		    OUTPUT_FIELD_TYPES,
		    new Serializable[] {
			    tedsResult.tedsID,
			    "</center>" + tedsResult.tedsHtmlString
				    + "<center>", status, tedsResult.fileName },
		    System.currentTimeMillis());
	    publishData(streamElement);
	} catch (RuntimeException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	    logger.error(new StringBuilder().append(" ********TEDS ERROR")
		    .toString());
	}
	try {
	    Thread.sleep(3000);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }

    private transient Collection<DataField> cachedOutputStructure = null;

    public Collection<DataField> getProducedStreamStructure() {
	if (cachedOutputStructure == null) {
	    cachedOutputStructure = new ArrayList<DataField>();
	    cachedOutputStructure.add(new DataField(ID_OUTPUT_FIELD,
		    "VARCHAR(20)", "Id of the detected transducer"));
	    cachedOutputStructure.add(new DataField(TEDS_OUTPUT_FIELD,
		    "VARCHAR(10000)", "TEDS-data"));
	    cachedOutputStructure.add(new DataField(STATUS_OUTPUT_FIELD,
		    "VARCHAR(20)", "status:added or removed"));
	    cachedOutputStructure.add(new DataField(VSFILE_OUTPUT_FIELD,
		    "VARCHAR(40)", "Virtual Sensor Filename"));
	}
	return cachedOutputStructure;
    }

    public void finalize(HashMap context) {
	super.finalize(context);
	threadCounter--;
    }

    private Object tedsCam1[][][] = {
    // chan0 - meta data about the TIM itself
	    { { MeasAttr.NAME, "CameraF" },
		    { MeasAttr.DESCRIPTION, "WirelessCamera" },
		    { MeasAttr.LOCATION, "INM 035" },
		    { MeasAttr.IP, "192.168.51.30" },
		    { MeasAttr.NUMBER_OF_CHANNELS, "1" },
		    { MeasAttr.MANUFACTURER, "GSN-LSIR-LAB" },
		    { MeasAttr.METADATA_ID, "Channel 0" } },
	    // chan1
	    { { MeasAttr.NAME, "CameraImage" },
		    { MeasAttr.DESCRIPTION, "Camera Picture" },
		    { MeasAttr.DATA_TYPE, "binary:JPEG" },
		    { MeasAttr.METADATA_ID, "Channel 1" } }, };

    private Object tedsCam2[][][] = {
    // chan0 - meta data about the TIM itself
	    { { MeasAttr.NAME, "CameraE" },
		    { MeasAttr.DESCRIPTION, "WirelessCamera" },
		    { MeasAttr.LOCATION, "INM 035" },
		    { MeasAttr.IP, "192.168.51.31" },
		    { MeasAttr.NUMBER_OF_CHANNELS, "1" },
		    { MeasAttr.MANUFACTURER, "GSN-LSIR-LAB" },
		    { MeasAttr.METADATA_ID, "Channel 0" } },
	    // chan1
	    { { MeasAttr.NAME, "IMAGE" },
		    { MeasAttr.DESCRIPTION, "Camera Picture" },
		    { MeasAttr.DATA_TYPE, "binary:JPEG" },
		    { MeasAttr.METADATA_ID, "Channel 1" } }, };

    private Object tedsCam3[][][] = {
    // chan0 - meta data about the TIM itself
	    { { MeasAttr.NAME, "CameraD" },
		    { MeasAttr.DESCRIPTION, "WirelessCamera" },
		    { MeasAttr.IP, "192.168.51.32" },
		    { MeasAttr.LOCATION, "INM 035" },
		    { MeasAttr.NUMBER_OF_CHANNELS, "1" },
		    { MeasAttr.MANUFACTURER, "GSN-LSIR-LAB" },
		    { MeasAttr.METADATA_ID, "Channel 0" } },
	    // chan1
	    { { MeasAttr.NAME, "IMAGE" },
		    { MeasAttr.DESCRIPTION, "Camera Picture" },
		    { MeasAttr.DATA_TYPE, "binary:JPEG" },
		    { MeasAttr.METADATA_ID, "Channel 1" } }, };

    private Object tedsCam4[][][] = {
    // chan0 - meta data about the TIM itself
	    { { MeasAttr.NAME, "CameraC" },
		    { MeasAttr.DESCRIPTION, "WirelessCamera:192.168.51.33" },
		    { MeasAttr.IP, "192.168.51.33" },
		    { MeasAttr.LOCATION, "INM 035" },
		    { MeasAttr.NUMBER_OF_CHANNELS, "1" },
		    { MeasAttr.MANUFACTURER, "GSN-LSIR-LAB" },
		    { MeasAttr.METADATA_ID, "Channel 0" } },
	    // chan1
	    { { MeasAttr.NAME, "IMAGE" },
		    { MeasAttr.DESCRIPTION, "Camera Picture" },
		    { MeasAttr.DATA_TYPE, "binary:JPEG" },
		    { MeasAttr.METADATA_ID, "Channel 1" } }, };

    private Object tedsCam5[][][] = {
    // chan0 - meta data about the TIM itself
	    { { MeasAttr.NAME, "CameraB" },
		    { MeasAttr.DESCRIPTION, "WirelessCamera:192.168.51.34" },
		    { MeasAttr.IP, "192.168.51.34" },
		    { MeasAttr.LOCATION, "INM 035" },
		    { MeasAttr.NUMBER_OF_CHANNELS, "1" },
		    { MeasAttr.MANUFACTURER, "GSN-LSIR-LAB" },
		    { MeasAttr.METADATA_ID, "Channel 0" } },
	    // chan1
	    { { MeasAttr.NAME, "IMAGE" },
		    { MeasAttr.DESCRIPTION, "Camera Picture" },
		    { MeasAttr.DATA_TYPE, "binary:JPEG" },
		    { MeasAttr.METADATA_ID, "Channel 1" } }, };

    private Object tedsCam6[][][] = {
    // chan0 - meta data about the TIM itself
	    { { MeasAttr.NAME, "CameraA" },
		    { MeasAttr.DESCRIPTION, "WirelessCamera:192.168.51.35" },
		    { MeasAttr.IP, "192.168.51.35" },
		    { MeasAttr.LOCATION, "INM 035" },
		    { MeasAttr.NUMBER_OF_CHANNELS, "1" },
		    { MeasAttr.MANUFACTURER, "GSN-LSIR-LAB" },
		    { MeasAttr.METADATA_ID, "Channel 0" } },
	    // chan1
	    { { MeasAttr.NAME, "IMAGE" },
		    { MeasAttr.DESCRIPTION, "Camera Picture" },
		    { MeasAttr.DATA_TYPE, "binary:JPEG" },
		    { MeasAttr.METADATA_ID, "Channel 1" } }, };
}
