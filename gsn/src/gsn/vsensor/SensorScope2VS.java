package gsn.vsensor;

import org.apache.log4j.Logger;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import java.util.TreeMap;
import java.text.NumberFormat;

public class SensorScope2VS extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger( SensorScope2VS.class );

    Double[] buffer;

    int length=-1;
    NumberFormat nf = NumberFormat.getInstance();

	public boolean initialize ( ) {
        VSensorConfig vsensor = getVirtualSensorConfiguration();
        //TreeMap<String, String> params = vsensor.getMainClassInitialParams();

        length = vsensor.getOutputStructure().length;

        buffer = new Double[length];

        /*
        String sampling_time = params.get("sampling");

        if (sampling_time != null) {
            try {
                Long st = Long.parseLong(sampling_time);
                SAMPLING_RATE = st.intValue();
            }
            catch (NumberFormatException e) {
                logger.debug(e.getMessage());
            }
        }
        logger.warn("Sampling rate : > " + SAMPLING_RATE + " <");
        */
		return true;
	}

	public void dataAvailable ( String inputStreamName , StreamElement data ) {


        logger.debug("Data => " + data.toString());

        // verify if all tuples are null, avoids duplicating data using buffer
        boolean nullpacket=true;
        for (int i=0;i<length;i++) {
            if ((Double)data.getData()[i]!=null)
                nullpacket = false;
        }
        if (nullpacket) return;



        for (int i=0;i<length;i++) {
            Double d = (Double)data.getData()[i];
            if (d!=null)
                buffer[i]=d;
        }

        /*
        * check if buffer contains any null values
        * */

        boolean publish=true;

        for (int i=0;i<length;i++) {
            data.setData(i,buffer[i]);
            if (buffer[i]==null) publish = false;
        }

        logger.debug("Pub => " + data.toString());

        if (publish) {
		    dataProduced( data );
		    if ( logger.isDebugEnabled( ) ) logger.debug( "Data received under the name: " + inputStreamName );
        }
        else {
            logger.debug("null values, not published ");
        }
	}

	public void dispose ( ) {

	}

}
