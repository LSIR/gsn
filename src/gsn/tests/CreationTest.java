package gsn.tests ;

import java.io.BufferedOutputStream ;
import java.io.ObjectOutputStream ;
import java.nio.ByteBuffer ;
import java.nio.charset.Charset ;
import java.nio.charset.CharsetDecoder ;
import java.nio.charset.CharsetEncoder ;
import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.Properties ;
import java.util.concurrent.ConcurrentHashMap ;

import org.apache.commons.collections.MultiHashMap ;
import org.apache.commons.collections.map.HashedMap ;
import org.apache.commons.io.output.ByteArrayOutputStream ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * 
 */
public class CreationTest {

   static int count = 10000 ;

   public static void main ( String [ ] args ) throws Exception {
      ArrayList < Object > list = new ArrayList < Object > ( ) ;
      long start , end ;

      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new Object ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for creating(Objects) " + list.size ( ) + " objects." ) ;
      list.clear ( ) ;

      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new HashMap ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for creating(HashMap) " + list.size ( ) + " objects." ) ;
      list.clear ( ) ;

      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new Properties ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for creating(Properties) " + list.size ( ) + " objects." ) ;
      list.clear ( ) ;

      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new String ( "FDSFSFSDFFDFD" ).getBytes ( "ISO-8859-1" ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for creating(String to bytes) " + list.size ( ) + " objects." ) ;

      list.clear ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new String ( "FDSFSFSDFFDFD" ).getBytes ( "ISO-8859-1" ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for creating(String to bytes) " + list.size ( ) + " objects." ) ;

      list.clear ( ) ;
      int capacity = 50 ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         ByteBuffer byteArray = ByteBuffer.allocate ( capacity ) ;
         list.add ( byteArray ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for creating(ByteBuffer.allocate( " + capacity + "))" + list.size ( ) + " objects." ) ;
      list.clear ( ) ;

      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         ByteBuffer byteArray = ByteBuffer.allocateDirect ( capacity ) ;
         list.add ( byteArray ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for creating(ByteBuffer.allocateDirect( " + capacity + "))" + list.size ( ) + " objects." ) ;
      list.clear ( ) ;

      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new ArrayList ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for creation(ArrayList) " + list.size ( ) + " objects." ) ;

      list.clear ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new HashedMap ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for creation(HashedMap) " + list.size ( ) + " objects." ) ;

      list.clear ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         list.add ( new MultiHashMap ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for creation(MultiMap by Commons-Coll) " + list.size ( ) + " objects." ) ;

      count = count / 10 ;

      Charset charset = Charset.forName ( "ISO-8859-1" ) ;
      CharsetDecoder decoder = charset.newDecoder ( ) ;
      CharsetEncoder encoder = charset.newEncoder ( ) ;
      ByteBuffer byteBuffer = ByteBuffer.allocateDirect ( 16 * 1024 ) ;

      System.out.println ( "******** Serialization and creation performance *********" ) ;
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ( 4 * 1024 ) ;
      ObjectOutputStream oos = new ObjectOutputStream ( ( new BufferedOutputStream ( byteArrayOutputStream , 4 * 1024 ) ) ) ;

      list.clear ( ) ;
      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new HashMap ( ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for serialization and creation(HashMap) " + count + " objects." ) ;

      list.clear ( ) ;
      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new Properties ( ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSec for serialization and creation(Properties) " + count + " objects." ) ;

      list.clear ( ) ;
      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new String ( "FDSFSFSDFFDFD" ).getBytes ( "ISO-8859-1" ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for serialization and creation(String) " + count + " objects." ) ;

      list.clear ( ) ;
      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new ArrayList ( ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for serialization and creation(ArrayList) " + count + " objects." ) ;

      list.clear ( ) ;
      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new MultiHashMap ( ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for serialization and creation(MultiHashMap) " + count + " objects." ) ;

      list.clear ( ) ;
      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new HashedMap ( ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( end - start + " MSecz for serialization and creation(HashedMap) " + count + " objects." ) ;

      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         oos.writeObject ( ( new ConcurrentHashMap ( ) ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.print ( end - start + " MSecz for serialization and creation(ConcurrentHashMap) " + count + " objects. " ) ;

      byteArrayOutputStream.reset ( ) ;
      start = System.currentTimeMillis ( ) ;
      for ( int i = 0 ; i < count ; i ++ ) {
         byteBuffer.wrap ( byteArrayOutputStream.toByteArray ( ) ) ;
      }
      end = System.currentTimeMillis ( ) ;
      System.out.println ( "Wrapping into byteArray will take : " + ( end - start ) ) ;
   }

}
