/**
 * 
 */
package gsn.utils.protocols.EPuck;

import gsn.utils.protocols.AbstractHCIQueryWithoutAnswer;

import java.util.Vector;


/**
 * @author alisalehi
 *
 */
public class SetSpeed extends AbstractHCIQueryWithoutAnswer {
	
	public static final String queryDescription = "Set the speed of the EPuck robot's two wheels.";
	public static final String[] paramsDescriptions = {"Speed of the left wheel.","Speed of the right wheel."};
   
	public SetSpeed (String name) {
      super(name, queryDescription, paramsDescriptions);
   }
   
   /*
    * This query takes exactly two Integer objects as parameters.
    * The first one sets the speed for the left wheel and the second
    * one sets the speed for the right wheel.
    * If there is an error with the parameters, the null pointer
    * is returned.
    */
   public byte[] buildRawQuery ( Vector < Object > params ) {
      byte[] query = null;
      if(params.size() == 2 && (params.get(0) instanceof Integer) && (params.get(1) instanceof Integer)) {
         Integer leftSpeed, rightSpeed;
         leftSpeed = (Integer)params.get(1);
         rightSpeed = (Integer)params.get(2);
         String textLeftSpeed = leftSpeed.toString( );
         String textRightSpeed = rightSpeed.toString( );
         query = new byte[3+textLeftSpeed.length()+textRightSpeed.length()];
         query[0] = 'D';
         query[1] = ',';
         byte[] bytesLeftSpeed = textLeftSpeed.getBytes();
         for(int i = 0; i < bytesLeftSpeed.length; i++ )
            query[2+i] = bytesLeftSpeed[i];
         query[2+bytesLeftSpeed.length]=',';
         byte[] bytesRightSpeed = textRightSpeed.getBytes();
         for(int i = 0; i < bytesRightSpeed.length; i++ )
            query[3+bytesLeftSpeed.length+i] = bytesRightSpeed[i];
      }
      return query;
   }
}
