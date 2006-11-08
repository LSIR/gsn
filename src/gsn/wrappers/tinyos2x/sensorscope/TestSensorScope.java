/**
 * 
 */
package gsn.wrappers.tinyos2x.sensorscope;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixSource;


/**
 * @author alisalehi
 *
 */
public class TestSensorScope {
   /**
    * A Test class to check if the wrapper works.
    * @param args
    * @throws Exception
    */
   public static void main ( String [ ] args ) throws Exception {
      PhoenixSource reader = BuildSource.makePhoenix( BuildSource.makeSF( "eflumpc24.epfl.ch" , 2020 ), null );
      reader.start( );
      MoteIF moteif = new MoteIF( reader );
      moteif.registerListener( new SensorScopeDataMsg( ) , new MessageListener( ) {
         public void messageReceived ( int dest , Message rawMsg ) {
            System.out.println( "Received." );
            //SensorScopeDataMsgWrapper msg = new SensorScopeDataMsgWrapper( ( SensorScopeDataMsg ) rawMsg );
            //msg.printMsg( );
         }
      } );
   }
}
