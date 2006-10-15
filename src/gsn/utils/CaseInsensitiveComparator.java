package gsn.utils;

import java.util.Comparator;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Note that this class will trim all the space characters surrounding the key
 * and value pairs hence you don't need to call trim when putting or getting a
 * value to/from the hashmap.
 */
public class CaseInsensitiveComparator implements Comparator {
   
   public int compare ( Object o1 , Object o2 ) {
      if ( o1 == null && o2 == null ) return 0;
      if ( o1 == null ) return -1;
      if ( o2 == null ) return 1;
      String input1 = o1.toString( ).trim( );
      String input2 = o2.toString( ).trim( );
      return input1.compareToIgnoreCase( input2 );
   }
}
