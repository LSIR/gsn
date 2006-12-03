package gsn.registry;

import gsn.registry.VSAddress;
import gsn.registry.VSAddress;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;


/**
 * Created on December 3, 2006, 2:14 AM
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class RegistryReferesh implements IoHandler{
   
   Set removalList = Collections.synchronizedSet (new HashSet ());
   
   List additionList = Collections.synchronizedList (new ArrayList<VSAddress>());
   
   Map<String,VSAddress> allItems = Collections.synchronizedMap (new LinkedHashMap<String,VSAddress>(10000,0.75f,true){
      protected boolean removeEldestEntry (Map.Entry eldest) {
         if (((VSAddress)eldest.getValue ()).getAge ()>Registry.REFERESH_INTERVAL){
            removeItem((VSAddress)eldest.getValue ());
            return true;
         }
         return false;
      }
   });
   
   private static final transient Logger   logger                           = Logger.getLogger ( RegistryReferesh.class );
   
   private Analyzer analyzer = new StandardAnalyzer ();
   
   private Directory directory;
   private static final int INITIAL_DELAY= 5000;
   private static final int LUCENE_UPDATE_INTERVAL = 500;//for testing,
   public RegistryReferesh (Directory directory) {
      this.directory = directory;
      Timer timer = new Timer ("Data Poster to Lucene");
      timer.scheduleAtFixedRate (new TimerTask () {
         public void run () {
            removeListOfVirtualSensors (removalList);
            addVirtualSensors (additionList);
         }
      },  INITIAL_DELAY,LUCENE_UPDATE_INTERVAL);
   }
   /**
    * Returns true if this method call causes a change in the content.
    */
   public synchronized boolean  addItem (VSAddress input){
      VSAddress old = allItems.put (input.getGUID (),input);
      if (old ==null || !old.equals (input)){
         additionList.add (input);
         return true;
      } else
         return false;
   }
   public synchronized void removeItem(VSAddress input){
      allItems.remove(input.getGUID());
      removalList.add(input);
   }
   
   public void sessionCreated (IoSession session) throws Exception {
      
   }
   
   public void sessionOpened (IoSession session) throws Exception {
      
   }
   
   public void sessionClosed (IoSession session) throws Exception {
      
   }
   
   public void sessionIdle (IoSession session, IdleStatus status) throws Exception {
      
   }
   
   public void exceptionCaught (IoSession session, Throwable cause) throws Exception {
      cause.printStackTrace ();
      session.close ();
   }
   
   public void messageReceived (IoSession session, Object newMsg) throws Exception {
      VSAddress input = (VSAddress) newMsg;
      input.setCreationTime (System.currentTimeMillis ( ));
      input.initGUID (((InetSocketAddress)session.getRemoteAddress ()).getHostName());
      boolean outcome = addItem (input);
      session.close ();   
   }
   
   public void messageSent (IoSession session, Object input) throws Exception {
      
   }
   
   public synchronized void removeListOfVirtualSensors ( Set < VSAddress > list ) {
      if ( list.isEmpty ( ) ) return;
      logger.fatal ("Remove CALLED");
      IndexReader indexReader;
      try {
         indexReader = IndexReader.open ( directory );
         for ( VSAddress address : list )
            indexReader.deleteDocuments ( new Term ( Registry._GUID , address.getGUID ( ) ) );
         list.clear();
         indexReader.close ( );
      } catch ( IOException e ) {
         logger.error ( e.getMessage ( ) , e );
      }
   }
   
   public synchronized void addVirtualSensors ( List<VSAddress> newVSs ) {
      try {         
         for (VSAddress newVS:newVSs){
            //logger.fatal ("Add CALLED");
            IndexWriter writer = new IndexWriter ( directory , analyzer, true );
            Document document = new Document ( );
            document.add ( new Field ( Registry._GUID , newVS.getGUID ( ) , Field.Store.NO , Field.Index.UN_TOKENIZED ) );
            document.add ( new Field ( Registry.DEFAULT_FIELD , newVS.getAddress () , Field.Store.NO , Field.Index.TOKENIZED ) );
            document.add ( new Field ( "key" , newVS.getAddressKeys () , Field.Store.NO , Field.Index.TOKENIZED ) );
            document.add ( new Field ( "value" , newVS.getAddressValues () , Field.Store.NO , Field.Index.TOKENIZED ) );
            document.add ( new Field ( "uses" , newVS.getUses () , Field.Store.NO , Field.Index.TOKENIZED ) );
            writer.addDocument ( document );
            writer.optimize ( );
            writer.close ( );
         }
      } catch ( IOException e ) {
         logger.fatal ( e.getMessage ( ) , e );
      }finally{
        newVSs.clear();        
      }
   }
   
}
