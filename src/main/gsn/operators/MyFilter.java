package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;

import java.util.List;

import org.apache.log4j.Logger;

public class MyFilter  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}


	private static final transient Logger logger = Logger.getLogger( BridgeVirtualSensor.class );
	private DataChannel outputChannel;

	public MyFilter(OperatorConfig config,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;

	}
	public void process ( String inputStreamName , StreamElement data ) {
		outputChannel.write(data );
		if ( logger.isDebugEnabled( ) ) logger.debug( "Data received under the name: " + inputStreamName );
	}

	public void dispose ( ) {

	}
	
}
