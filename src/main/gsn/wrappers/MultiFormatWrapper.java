package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * This wrapper presents a MultiFormat protocol in which the data comes from the
 * system clock. Think about a sensor network which produces packets with
 * several different formats. In this example we have 3 different packets
 * produced by three different types of sensors. Here are the packet structures
 * : [temperature:double] , [light:double] , [temperature:double, light:double]
 * The first packet is for sensors which can only measure temperature while the
 * latter is for the sensors equipped with both temperature and light sensors.
 * 
 */
public class MultiFormatWrapper implements Wrapper {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private DataField[] collection = new DataField[] { new DataField("packet_type", "int"),
			new DataField("temperature", "double"), new DataField("light", "double") };
	private final transient Logger logger = Logger.getLogger(MultiFormatWrapper.class);

	private long rate = 1000;

	public MultiFormatWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		rate = conf.getParameters().getPredicateValueAsInt("rate", 1000);
	}

	public void start() {
		Double light = 0.0, temperature = 0.0;
		int packetType = 0;

		while (isActive) {
			try {
				// delay 
				Thread.sleep(rate);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}

			// create some random readings
			light = ((int) (Math.random() * 10000)) / 10.0;
			temperature = ((int) (Math.random() * 1000)) / 10.0;
			packetType = 2;

			// post the data to GSN
      StreamElement se = StreamElement.from(this).setTime(System.currentTimeMillis());
      Serializable[] values = {packetType, temperature, light};
      for (int i=0;i<getOutputFormat().length;i++)
        se.set(getOutputFormat()[i].getName(),values[i]);
      
      dataChannel.write(se);
		}
	}

	public DataField[] getOutputFormat() {
		return collection;
	}

	private boolean isActive;

	public void dispose ( ) {
	}

	public void stop() {
		isActive = false;
	}

}
