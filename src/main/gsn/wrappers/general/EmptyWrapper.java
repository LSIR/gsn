package gsn.wrappers.general;

import gsn.beans.DataField;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import org.apache.log4j.Logger;

public class EmptyWrapper implements Wrapper {
	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private final transient Logger               logger        = Logger.getLogger( EmptyWrapper.class );


	private static   DataField [] dataField  ;

	public EmptyWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		dataField = new DataField[] { new DataField( "DATA" , "int" ) };
	}

	public void start(){
		while ( isActive ) {
			// do something
		}
	}

	public  DataField[] getOutputFormat ( ) {
		return dataField;
	}
	private boolean isActive=true;

	public void dispose ( ) {
		
	}

	public void stop() {
		isActive = false;
	}

}
