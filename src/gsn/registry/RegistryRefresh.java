package gsn.registry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Created on December 3, 2006, 2:14 AM
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class RegistryRefresh implements Runnable {
   
   private final transient Logger  logger                 = Logger.getLogger( RegistryRefresh.class );
   
   private Analyzer                analyzer               = new StandardAnalyzer( );
   
   private IndexWriter             writer;
   
   private final int               LUCENE_UPDATE_INTERVAL = 2000;                                       // for
   
   public static final String      DEFAULT_FIELD          = "default_field";
   
   private QueryParser             queryParser            = new QueryParser( DEFAULT_FIELD , analyzer );
   
   private IndexSearcher           searcher;
   private Directory               directory;
   
   public RegistryRefresh ( String dirPath ) throws IOException {
      if ( dirPath == null ) {
         directory = new RAMDirectory( );
         logger.warn( "Using RamDirectory which is not persistant." );
      } else {
         directory = FSDirectory.getDirectory( dirPath , false );
      }
      writer = new IndexWriter( directory , analyzer , true );
      searcher = new IndexSearcher( directory );
   }
   
   public void run ( ) {
      while ( true ) {
         try {
            Thread.sleep( LUCENE_UPDATE_INTERVAL );
         } catch ( InterruptedException e ) {
            logger.error( e.getMessage( ) , e );
         }
          
         try {
        	 Collection<VSAddress> newVSList = RegistryRequestHandler.getList( ).values();
             IndexReader reader = IndexReader.open(directory);
        	 for ( VSAddress newVS : newVSList ) {
             	if(reader.termDocs(new Term(Registry._GUID,newVS.getGUID()))!=null) {
             		reader.deleteDocuments(new Term(Registry._GUID,newVS.getGUID()));
             	}
        	 }
        	 reader.close();

            for ( VSAddress newVS : newVSList ) {
               // logger.fatal ("Add CALLED");
               Document document = new Document( );
               document.add( new Field( Registry._GUID , newVS.getGUID( ) , Field.Store.YES , Field.Index.UN_TOKENIZED ) );
               document.add( new Field( DEFAULT_FIELD , newVS.getDefaultField( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
               document.add( new Field( "key" , newVS.getAddressKeys( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
               document.add( new Field( "value" , newVS.getAddressValues( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
               document.add( new Field( "uses" , newVS.getUses( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
               document.add( new Field( "description" , newVS.getDescription( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
               writer.addDocument( document );
            }
         } catch ( IOException e ) {
            logger.error( e.getMessage( ) , e );
         }
         try {
            writer.optimize( );
            writer.close( );
         } catch ( IOException e ) {
            logger.error( e.getMessage( ) , e );
         }
         try{
        	 searcher = new IndexSearcher( directory );
         } catch ( IOException e ) {
            logger.error( e.getMessage( ) , e );
         }
      }
   }
   
   /**
    * Returns null if the resultset is empty.
    */
   public Hits doQuery ( String queryString ) throws ParseException {
      try {
         Query query = queryParser.parse( queryString );
         return searcher.search( query );
      } catch ( IOException e ) {
         logger.error( e.getMessage( ) , e );
         return null;
      }
   }
}
