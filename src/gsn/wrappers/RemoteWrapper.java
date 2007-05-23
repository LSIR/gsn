package gsn.wrappers;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.AddressBean;
import gsn.beans.ContainerConfig;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * TODO : Optimization for combining where clauses in the time based window and updating the
 * registered query with the new where clause
 */
public class RemoteWrapper extends AbstractWrapper {

	private final transient Logger     logger                 = Logger.getLogger ( RemoteWrapper.class );

	private  DataField[]    strcture               = null;

	private String                     remoteVSName;

	private ArrayList < CharSequence > registeredWhereClauses = new ArrayList < CharSequence >( );

	private String                     host;

	private int                        port;

	private XmlRpcClient                    client             = new XmlRpcClient ( );

	private  XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl ( );

	private String url;

	public boolean initialize (  ) {
		/**
		 * First looks for URL parameter, if it is there it will be used otherwise
		 * looks for host and port parameters.
		 */
		AddressBean addressBean =getActiveAddressBean ( );
		if ( (url =addressBean.getPredicateValue ( "url" ))==null) {
			host = addressBean.getPredicateValue ( "host" );
			if ( host == null || host.trim ( ).length ( ) == 0 ) {
				logger.warn ( "The >host< parameter is missing from the RemoteWrapper wrapper." );
				return false;
			}
			int port = addressBean.getPredicateValueAsInt("port" ,ContainerConfig.DEFAULT_GSN_PORT);
		    if ( port > 65000 || port <= 0 ) {
		    	logger.error("Remote wrapper initialization failed, bad port number:"+port);
		    	return false;
		    }
		    
		     	 url ="http://" + host +":"+port;
		}
		url+="/gsn-handler";   
	     
	   this.remoteVSName = addressBean.getPredicateValue ( "name" );
		if ( this.remoteVSName == null ) {
			logger.warn ( "The \"NAME\" paramter of the AddressBean which corresponds to the remote Virtual Sensor is missing" );
			return false;
		}
		
		this.remoteVSName = remoteVSName.trim ().toLowerCase ();
		try {
			config.setServerURL ( new URL ( url ) );
			client.setConfig ( config );
		} catch ( MalformedURLException e1 ) {
			logger.warn ( "Remote Wrapper initialization failed : "+e1.getMessage ( ) , e1 );
		}
		this.strcture = askForStrcture ( );
		if ( this.strcture == null ) {
			logger.warn ( "The initialization of the ** virtual sensor failed due to *askForStrcture* failure." );
			return false;
		}
		Mappings.getContainer ( ).addRemoteStreamSource ( getDBAlias ( ) , this );
		return true;
	}

	/**
	 * @return Null if the RemoteWrapper can't obtain the data strcture from the
	 */
	private  DataField [] askForStrcture ( ) {
		String destination = new StringBuilder ( ).append ( host ).append ( ":" ).append ( port ).toString ( );
		if ( logger.isInfoEnabled ( ) ) logger.info ( new StringBuilder ( ).append ( "Wants to ask for structure from : " ).append ( destination ).toString ( ) );
		Object [ ] params = new Object [ ] {remoteVSName};
		Object[] result =null;
		try{
			result =  (Object[]) client.execute ("gsn.getOutputStructure", params);
		}catch(Exception e){
			logger.warn ( new StringBuilder ( ).append ( "Message couldn't be sent to :" ).append (destination).append (", ERROR : ").append (e.getMessage ()).toString ( ) );
			logger.debug (e.getMessage (),e);
			return null;
		}
		if ( result.length==0) {
			logger.warn ( new StringBuilder ( ).append ( "Message couldn't be sent to :" ).append (destination).toString ( ) );
			return null;
		}
		DataField[] toReturn = new DataField [result.length];
		for (int i=0;i<result.length;i++){
			Object values [] = (Object[]) result[i];
			toReturn[i]= new DataField (values[0].toString (),values[1].toString (),"");
		}
		return toReturn;
	}
	/**
	 * First deregister then register
	 */
	private void refreshRemotelyRegisteredQuery ( ) throws XmlRpcException {
		int notificationCode = getDBAlias ( );
//		String query = new StringBuffer ( "SELECT * FROM " ).append ( remoteVSName ).append ( " WHERE " ).append ( getWhereClausesAllTogher ( ) ).append ( " ORDER BY " ).append ( remoteVSName ).append (
//		".TIMED DESC LIMIT 1 OFFSET 0" ).toString ( );
		String query = new StringBuilder("select * from ").append(remoteVSName).toString();
		Object [ ] params = new Object [ ] {Main.getContainerConfig ().getContainerPort (),remoteVSName,query.toString( ), notificationCode};
		if ( logger.isDebugEnabled ( ) )
			logger.debug ( new StringBuilder ( ).append ( "Wants to send message to : " ).append ( host ).append (port).append ("/").append (remoteVSName).append ( " with the query ->" ).append ( query ).append ( "<-" ).toString ( ) );
		Boolean bool = (Boolean) client.execute ("gsn.registerQuery", params);
		if (bool==false) {
			logger.warn ( new StringBuilder ( ).append ( "Query Registeration for the remote virtual sensor : ").append (remoteVSName).append (" failed.").toString ( ) );
			return ;
		}
	}
	/**
	 * Note that query translation is not needed, it is going to performed in the receiver's side.
	 * @throws SQLException 
	 */
	public void addListener ( StreamSource ss ) throws SQLException {
		super.addListener(ss);
		try {
			registeredWhereClauses.add(ss.toSql());
			refreshRemotelyRegisteredQuery();
		}
		catch (XmlRpcException ex) {
			logger.warn("Adding the data listener failed. "+ex.getMessage(), ex);
		};
	}

	public void removeListener ( StreamSource ss ) throws SQLException {
		super.removeListener ( ss );
		registeredWhereClauses.remove ( ss );
	}

	/**
	 * The container is going to notify the <code>RemoteWrapper</code> about arrival
	 * of a new data by calling this method. (note that, container will first
	 * insert the data into the appropriate database and then calls the following
	 * method).
	 */
	public boolean remoteDataReceived (StreamElement se ) {
		return postStreamElement(se);
	}

	public  DataField [] getOutputFormat ( ) {
		return strcture;
	}

	private String getWhereClausesAllTogher ( ) {
		StringBuffer result = new StringBuffer ( );
		for ( CharSequence whereClause : registeredWhereClauses ) {
			result.append ( "(" ).append ( whereClause ).append ( ")" ).append ( " OR " );
		}
		return result.delete ( result.length ( ) - " OR ".length ( ) , result.length ( ) ).toString ( );
	}

	public void finalize ( ) {
		//TODO
	}
	public final String getRemoteHost (){
		return host;
	}
	public int getRemotePort (){
		return port;
	}
	public final String getRemoveVSName (){
		return remoteVSName;
	}

	public String getWrapperName () {
		return "Remote source GSN network";
	}
}