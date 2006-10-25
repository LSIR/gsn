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
	
	public abstract AbstractHCIQuery getQuery(String queryName);
   
   /*
    * Returns null if the query does not exists, and the raw bytes
    * to send to the wrapper if the query has been found.
    */
   public abstract byte[] buildRawQuery(String queryName, Vector<Object> params);
}
