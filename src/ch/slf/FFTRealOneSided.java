package ch.slf;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;

import org.apache.commons.math.MathException;
import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.transform.FastFourierTransformer;
import org.apache.log4j.Logger;

/**
 * <p>
 * This Virtual Sensor computes the FFT over an array of data.
 * The input array must contain a power of 2 elements. If any other number of elements is used, the array will be zero-padded to the next power of 2 elements.
 * </p>
 * <p>
 * The output format is a 1 sided real FFT normalised using 1/sqrt(N). 
 * This algorithm uses the Cooley-Tukey FFT method. 
 * The algorithm returns N/2 +1 points where the first point is the DC component.
 * </p>
 * <p>
 * This Virtual Sensor uses the free Apache Math Library for computation.
 * Have a look to <a href="http://commons.apache.org/math/apidocs/index.html">Apache Math Library</a>
 * for its documentation.
 * </p>
 */
public class FFTRealOneSided extends WindowAwareVS {

	private static final String DF = "DF";

	private static final String VALUES = "VALS";

	private static final DataField [] outputStructure = new DataField[] { 
		new DataField(DF, DataTypes.DOUBLE_NAME), 
		new DataField(VALUES, "BINARY:text/plain")
	};

	private static transient Logger logger  = Logger.getLogger ( FFTRealOneSided.class );

	private static FastFourierTransformer fft;

	private int fft_size;


	public boolean init() {
		try {
			fft_size = getPredicateValueAsIntWithException("window-size");
			fft = new FastFourierTransformer () ;
			if (! FastFourierTransformer.isPowerOf2(fft_size)) {
				logger.error("The window size >" + fft_size + "< is not a power of 2.");
				return false;
			}
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
	public void process(double [] values , long[] timestampsInMSec) {

		if (logger.isDebugEnabled()) {
			logger.debug("INPUT FFT DATA");
			for (int i = 0 ; i < values.length ; i++) {
				logger.debug(values[i] + "\n");
			}
		}

		int windowSize = timestampsInMSec.length;
		logger.debug("Window Size: " + windowSize);

		long deltaTimeStampInSec = (timestampsInMSec[timestampsInMSec.length - 1] - timestampsInMSec[0]) / 1000;
		logger.debug("Delta Time Stamp in s: " + deltaTimeStampInSec);

		double sampling_rate = ((double)windowSize / (double)deltaTimeStampInSec);
		logger.debug("Sampling Rate: " + sampling_rate);

		double df = 1 / (double)deltaTimeStampInSec;
		logger.debug("df: " + df);

		int nbOfPointsToReturn = (windowSize / 2) + 1;
		logger.debug("Number of points to return: " + nbOfPointsToReturn);
		
		long middleTimeStamp = timestampsInMSec[0] + ((deltaTimeStampInSec / 2) * 1000);

		Complex[] fftResult = null;
		try {

			fftResult = fft.transform2(values);

			double[] realPartFftResult = new double [nbOfPointsToReturn] ;
			for (int i = 0 ; i < realPartFftResult.length ; i++) {
				realPartFftResult[i] = fftResult[i].getReal();
			}

			//
			Serializable[] dataOut = new Serializable[2];
			// df
			dataOut[0] = df;
			// data
			StringBuilder sb = new StringBuilder();
			sb.append(realPartFftResult[0]);
			for (int i = 1 ; i < realPartFftResult.length ; i++) {
				sb.append("," + realPartFftResult[i]);
			}
			dataOut[1] = sb.toString().getBytes();

			//
			StreamElement se = new StreamElement (outputStructure, dataOut, middleTimeStamp);
			logger.debug("FFT StreamElement produced: " + se);
			dataProduced( se );

		} catch (IllegalArgumentException e) {
			logger.error("Unable to compute the FFT: " + e.getMessage());
		} catch (MathException e) {
			logger.error("Unable to compute the FFT: " + e.getMessage());
		}
	}
}
