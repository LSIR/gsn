package gsn.registry ;

import gsn.shared.VirtualSensorIdentityBean ;
import gsn.utils.KeyValueImp ;

import java.util.ArrayList ;

import org.apache.commons.collections.KeyValue ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class MatchingTest {

   public static void main ( String [ ] args ) {
      ArrayList < KeyValue > predicates1 = new ArrayList < KeyValue > ( ) ;
      predicates1.add ( new KeyValueImp ( "key1" , "value1" ) ) ;
      predicates1.add ( new KeyValueImp ( "key2" , "value2" ) ) ;
      VirtualSensorIdentityBean bean1 = new VirtualSensorIdentityBean ( "s1" , "1.2.3.4" , 20000 , predicates1 ) ;

      ArrayList < KeyValue > predicates2 = new ArrayList < KeyValue > ( ) ;
      predicates2.add ( new KeyValueImp ( "key1" , "value1" ) ) ;
      predicates2.add ( new KeyValueImp ( "key3" , "value3" ) ) ;
      VirtualSensorIdentityBean bean2 = new VirtualSensorIdentityBean ( "s2" , "1.2.3.5" , 20001 , predicates2 ) ;

      ArrayList < KeyValue > predicateTest = new ArrayList < KeyValue > ( ) ;
      predicateTest.add ( new KeyValueImp ( "keY3" , "Value3" ) ) ;
      // predicateTest.add(new KeyValueImp("key1","value1"));

      System.out.println ( bean1.matches ( predicateTest ) ) ;
      System.out.println ( bean2.matches ( predicateTest ) ) ;

   }

}
