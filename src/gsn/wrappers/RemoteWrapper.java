package gsn.wrappers;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class RemoteWrapper extends AbstractWrapper {
   
   private final transient Logger     logger                 = Logger.getLogger ( RemoteWrapper.class );
   
   private  DataField[]    strcture               = null;
   
   private String                     remoteVSName;
   
   private ArrayList < StringBuffer > registeredWhereClauses = new ArrayList < StringBuffer >( );
   
   private String                     host;
   
   private int                        port;
   
   private XmlRpcClient                    client             = new XmlRpcClient ( );
   
   private  XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl ( );
   
   public boolean initialize (  ) {
      AddressBean addressBean =getActiveAddressBean ( );
      host = addressBean.getPredicateValue ( "host" );
      if ( host == null || host.trim ( ).length ( ) == 0 ) {
         logger.warn ( "The >host< parameter is missing from the RemoteWrapper wrapper." );
         return false;
      }
      String portRaw = addressBean.getPredicateValue ( "port" );
      if ( portRaw == null || portRaw.trim ( ).length ( ) == 0 ) {
         logger.warn ( "The >port< parameter is missing from the RemoteWrapper wrapper." );
         return false;
      }
      try {
         port = Integer.parseInt ( portRaw );
         if ( port > 65000 || port <= 0 ) throw new Exception ( "Bad port No" + port );
      } catch ( Exception e ) {
         logger.warn ( "The >port< parameter is not a valid integer for the RemoteWrapper wrapper." );
         return false;
      }
      this.remoteVSName = addressBean.getPredicateValue ( "name" );
      if ( this.remoteVSName == null ) {
         logger.warn ( "The \"NAME\" paramter of the AddressBean which corresponds to the remote Virtual Sensor is missing" );
         return false;
      }
      this.remoteVSName = this.remoteVSName.trim ().toLowerCase ();
      try {
         config.setServerURL ( new URL ( "http://" + host +":"+port+ "/gsn-handler" ) );
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
      StringBuilder query = new StringBuilder ( "SELECT * FROM " ).append ( remoteVSName ).append ( " WHERE " ).append ( getWhereClausesAllTogher ( ) ).append ( " ORDER BY " ).append ( remoteVSName ).append (
              ".timed DESC " );
      /**
       * Ali : Q, why we need LIMIT 1 OFFSET 0 ?
       * A: For each stream element produced the remote virtual
       * sensor will evaluate the queries and since we want to first have the clone of the
       * DB at our side and then perform the query. By clone, I mean any data time that
       * satisfies the union of the user defined filtering conditions, minimum of the start
       * time and maximum of the end time, and the rate limits. Note that we don't enforce
       * the window predicates because we want to first store the useful data in the local
       * machine and then use the windows to calculate the queries.
       */
      query.append( " LIMIT 1 OFFSET 0" );  
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
    */
   public CharSequence addListener ( DataListener dataListener ) {
      try {
          StringBuffer completeMergedWhereClause = dataListener.getCompleteMergedWhereClause(remoteVSName);
          registeredWhereClauses.add(completeMergedWhereClause);
          refreshRemotelyRegisteredQuery();
          return super.addListener(dataListener);
      }
      catch (XmlRpcException ex) {
         logger.warn("Adding the data listener failed. "+ex.getMessage(), ex);
      };
      return null;
   }
   
   public void removeListener ( DataListener dataListener ) {
      registeredWhereClauses.remove ( dataListener.getCompleteMergedWhereClause ( remoteVSName ) );
    // TEST REQUIRED, I'm not sure once the listener is removed, gsn answers with nak to the data packet.  if ( registeredWhereClauses.size ( ) > 0 ) refreshRemotelyRegisteredQuery ( );
      super.removeListener ( dataListener );
   }
   
   /**
    * The container is going to notify the <code>RemoteWrapper</code> about arrival
    * of a new data by calling this method. (note that, container will first
    * insert the data into the appropriate database and then calls the following
    * method).
    */
   public void remoteDataReceived ( ) {
      if ( logger.isDebugEnabled ( ) ) logger.debug ( "There are results, is there any listeners ?" );
      for ( Iterator < DataListener > iterator = listeners.iterator ( ) ; iterator.hasNext ( ) ; ) {
         DataListener dataListener = iterator.next ( );
         boolean results = getStorageManager ( ).isThereAnyResult ( dataListener.getViewQuery ( ) );
         if ( results ) {
            if ( logger.isDebugEnabled ( ) )
               logger.debug ( new StringBuilder ( ).append ( "There are listeners, notify the " ).append ( dataListener.getInputStream ( ).getInputStreamName ( ) ).append ( " inputStream" ).toString ( ) );
            // TODO :DECIDE WHETHER TO INFORM THE CLIENT OR NOT (TIME
            // TRIGGERED. DATA TRIGGERED)
            dataListener.dataAvailable ( );
         }
      }
   }
   
   public  DataField [] getOutputFormat ( ) {
      return strcture;
   }
   
   private String getWhereClausesAllTogher ( ) {
      StringBuffer result = new StringBuffer ( );
      for ( StringBuffer whereClause : registeredWhereClauses ) {
         result.append ( "(" ).append ( whereClause ).append ( ")" ).append ( " OR " );
      }
      return result.delete ( result.length ( ) - " OR ".length ( ) , result.length ( ) ).toString ( );
   }
   
   public void finalize ( ) {
      //TODO
   }
   public String getRemoteHost (){
      return host;
   }
   public int getRemotePort (){
      return port;
   }
   public String getRemoveVSName (){
      return remoteVSName;
   }
   
   public String getWrapperName () {
      return "Remote source GSN network";
   }
}