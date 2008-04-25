package ch.slf;

import org.apache.log4j.Logger;

/**
 * <p>
 * Use the Common Math library developed by Apache in order to do extended
 * mathematical and statistical computations.<br />
 * The API is available on the web: <a href="http://commons.apache.org/math/apidocs/index.html">Apache Math Library</a>
 * </p>
 */
public class FFTSample extends WindowAwareVS {

	private static transient Logger logger  = Logger.getLogger ( FFTSample.class );

	double fft_size;

	double overlap ;

	public boolean init() {
		try {
			fft_size = getPredicateValueAsIntWithException("window-size");
			overlap  = (getPredicateValueAsIntWithException("step-size") / fft_size) * 100.0;
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			return false;
		}
		return true;
	}

	/**
	 * @param values A two dimensional array that contains in first dimension (values[])
	 *        the differents steps, and in the second dimension (value[step][]) the different
	 *        measures for this step.
	 * @param timestamps An array that contains the timestamps in milli seconds at every
	 *        step.
	 * @return
	 */
	public FFTOutputPacket process(double [][] values , long[] timestampsInMSec) {

		if (logger.isDebugEnabled()) {
			logger.debug("FFTSample process method called with " + values.length + " steps of " + values[0].length + " measures");
			// Print out the Datas to process
			StringBuffer sb = new StringBuffer();
			for (int i = 0 ; i < values.length ; i++) {
				sb.append("\n\n" + timestampsInMSec[i] + " | ");
				for (int j = 0 ; j < values[i].length ; j++) {
					sb.append(values[i][j] + " ");
				}
				sb.append("\n");
			}
			sb.append("\n");
			logger.debug(sb.toString());
		}
		
		//double sampling_rate = 1/((double)(timestampsInMSec[timestampsInMSec.length-1] - timestampsInMSec[0])/timestampsInMSec.length)*1000;
		
		FFTOutputPacket out = new FFTOutputPacket(timestampsInMSec[timestampsInMSec.length/2]);
		double[] results = new double [] {1,2,3,0.0, 4, 5, 6} ;
		out.setdf(1.2);
		out.addValues(results);

		return out;
	}
}
