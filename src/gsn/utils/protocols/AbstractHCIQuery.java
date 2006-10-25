/**
 * 
 * @author Jerome Rousselot
 */
package gsn.utils.protocols;

import java.util.Vector;
import java.util.regex.Pattern;

/**
 *  This class represents a query of a host controller interface protocol.
 *  Such a protocol can be used to communicate with a node connected
 *  to a computer through a hardware interface (bluetooth, usb, serial port,
 *  parallel port...) using higher-level abstractions.
 *  This means that instead of writing the appropriate bytes to a port to
 *  get a node ID, you can send the AbstractHCIQuery "getID".
 *  This makes the code easier to read and to maintain.
 *  @author Jérôme Rousselot <jerome.rousselot@csem.ch>
 *
 */
public abstract class AbstractHCIQuery {
	
	public static final int NO_WAIT_TIME = 0;
	protected static Pattern answerPattern = Pattern.compile(".*\\z");
	
	/*
	 * This method returns the name of this query.
	 * 
	 */
	public abstract String getName();
	/*
	 * This method takes a Vector of arguments as input and
	 * produces the raw data to be sent to the wrapper.
	 * Note that elements of a Vector are ordered, and that
	 * the Vector can be null.
	 * @param params A vector of Object containing the required
	 * parameters for this query. This should be described in
	 * the implementation.
	 * 
	 */
	public abstract byte[] buildRawQuery(Vector<Object> params);
	
	/*
	 * This method tells us whether we should wait for an answer
	 * from the mote if we send a query with these parameters.
	 * @param params A vector of Object containing the required
	 * parameters for this query. This should be described in
	 * the implementation.
	 * 
	 */
	public abstract boolean needsAnswer(Vector<Object> params);
	
	/*
	 * This method tells us how much time we should wait for an
	 *  answer from the mote if we send a query with these 
	 *  parameters.
	 * @param params A vector of Object containing the required
	 * parameters for this query. This should be described in
	 * the implementation.
	 *
	 */
	public abstract int getWaitTime(Vector<Object> params);

	/*
	 * This method returns a regex pattern that can match
	 * the answer from this query. By default it is just a greedy
	 * matcher: it tries to match as much data as it can.
	 * The default should be ok for most applications, but
	 * tuning this can make your software more robust.
	 * @param params A vector of Object containing the required
	 * parameters for the query. This should be described in
	 * the implementation. This can be null.
	 *
	 */
	
	public Pattern getAnswerPattern(Vector<Object> params) {
		return answerPattern;
	}
	
}
