/**
 * 
 * @author Jerome Rousselot
 */
package gsn.utils.protocols;


import java.util.Vector;

/**
 * @author Jérôme Rousselot <jerome.rousselot@csem.ch>
 *
 */
public abstract class AbstractHCIProtocol {

	/*
	 * Returns the name of the protocol represented
	 * by this class.
	 * 
	 */
	public abstract String getName();
	
	/*
	 * Returns the complete list of all queries known 
	 * by this protocol.
	 */
	public abstract Vector<AbstractHCIQuery> getQueries();
	
	
}
