package gsn.wrappers;

import org.apache.log4j.Logger;
import gsn.beans.DataField;

/**
 * This wrapper presents a MultiFormat protocol in which the data comes from the system clock.
 * Think about a sensor network which produces packets with several differnet formats.
 * In this example we have 3 different packets produced by three different types of sensors.
 * Here are the packet structures : 
 * [temperature:double] , [light:double] , [temperature:double, light:double]
 * The first packet is for sensors which can only measure temperature while the latter is for the
 * sensors equipped with both temperature and light sensors. 
 * 
 */
public class MultiFormatWrapper extends AbstractWrapper{
	private  DataField   []    collection        = new DataField[] {new DataField("packet_type","int","packet type"),new DataField("temperature","double","Presents the temperature sensor."),new DataField("light","double","Presents the light sensor.")};
	private final transient Logger        logger            = Logger.getLogger( MultiFormatWrapper.class );
	private int counter;

	public void finalize() {
		counter--;
	}

	public DataField[] getOutputFormat() {
		return collection;
	}

	public String getWrapperName() {
		return "MultiFormat Sample Wrapper";
	}

	public boolean initialize() {
		setName("MultiFormatWrapper"+counter++);
		return true;
	}

	public void run() {
		while (isActive()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(),e);
			}finally {
				if (getListeners().size()==0)
					continue;
			}
			long currentTime = System.currentTimeMillis();
			Double light=null,temperature=null;
			int packetType = 0;
			if (currentTime%17 ==0) {
				light=((int)(Math.random()*10000))/10.0;
				packetType=1;
			}
			if (currentTime%19 ==0) {
				temperature = ((int)(Math.random()*1000))/10.0;
				packetType=2;
			}
			if (packetType>0) {
				postStreamElement(packetType,temperature,light);
			}

		}
	}   
}
