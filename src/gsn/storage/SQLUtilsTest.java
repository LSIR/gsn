package gsn.storage;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class SQLUtilsTest extends TestCase {
   
   protected void setUp ( ) throws Exception {
      super.setUp( );
   }
   
   protected void tearDown ( ) throws Exception {
      super.tearDown( );
   }
   
   /*
    * Test method for 'gsn.storage.SQLUtils.rewriteQuery(String, HashMap<String,
    * String>)'
    */
   public void testRewriteQuery ( ) {

   }
   
   /*
    * Test method for
    * 'gsn.storage.SQLUtils.extractTableNamesUsedInQuery(String)'
    */
   public void testExtractTableNamesUsedInQuery ( ) {
      StringBuilder query = new StringBuilder( "select * from bla " );
      ArrayList < String > output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 1 );
      assertEquals( output.get( 0 ) , "BLA" );
      query = new StringBuilder( "select * from bla,bla2" );
      output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 2 );
      assertEquals( output.get( 0 ) , "BLA" );
      assertEquals( output.get( 1 ) , "BLA2" );
      query = new StringBuilder( "select * from bla ,bla2" );
      output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 2 );
      assertEquals( output.get( 0 ) , "BLA" );
      assertEquals( output.get( 1 ) , "BLA2" );
      
      query = new StringBuilder( "select * from bla , bla2" );
      output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 2 );
      assertEquals( output.get( 0 ) , "BLA" );
      assertEquals( output.get( 1 ) , "BLA2" );
      
      query = new StringBuilder( "select * from bla , bla2 where ali" );
      output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 2 );
      assertEquals( output.get( 0 ) , "BLA" );
      assertEquals( output.get( 1 ) , "BLA2" );
      
      query = new StringBuilder( "select * from bla , bla2 as re where ali" );
      output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 2 );
      assertEquals( output.get( 0 ) , "BLA" );
      assertEquals( output.get( 1 ) , "BLA2" );
      
      query = new StringBuilder( "select * from bla as j, bla2 where ali" );
      output = SQLUtils.extractTableNamesUsedInQuery( query );
      assertEquals( output.size( ) , 2 );
      assertEquals( output.get( 0 ) , "BLA" );
      assertEquals( output.get( 1 ) , "BLA2" );
      
   }
   
}
