package gsn.wrappers.wsn.simulator;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class WSNSWrapper implements Wrapper , DataListener {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private final transient Logger   logger                     = Logger.getLogger( WSNSWrapper.class );

	/**
	 * The rate, set the rate in which the network is re-revaluated. If the rate
	 * is 1000, then the network is reevaluated every seconds. If the rate is
	 * negative, the network will stop evaluating it self after specified number
	 * of cycles.
	 */
	private static String            RATE_KEY                   = "rate";

	/**
	 * The Rate is specified in msec.
	 */
	private static int               RATE_DEFAULT_VALUE         = 2000;

	private int                      rate                       = RATE_DEFAULT_VALUE;

	private static int               NODE_COUNT_DEFAULT_VALUE   = 10;

	private static String            NODE_COUNT_KEY             = "node_count";

	private int                      node_count                 = NODE_COUNT_DEFAULT_VALUE;

	private WirelessNode [ ]         nodes;

	private ArrayList < DataPacket > dataBuffer                 = new ArrayList < DataPacket >( );

	private String                   STEP_COUNTER               = "steps";

	private static final int         STEP_COUNTER_DEFAULT_VALUE = -1;

	private int                      step_counter               = STEP_COUNTER_DEFAULT_VALUE;

	public WSNSWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		/**
		 * Reading the initialization paramteters from the XML Configurations
		 * provided.
		 */
		node_count = conf.getParameters().getValueAsInt( NODE_COUNT_KEY,NODE_COUNT_DEFAULT_VALUE ) ;
		step_counter = conf.getParameters().getValueAsInt( STEP_COUNTER,STEP_COUNTER_DEFAULT_VALUE ) ;
		rate = conf.getParameters().getValueAsInt( RATE_KEY,RATE_DEFAULT_VALUE ) ;

		if ( step_counter <= 0 ) 
			throw new RuntimeException( "The specified >step_counter< parameter for the >WSNWrapper< shouldn't be a negative number.\nGSN disables the step_counter (-1)." );

	}

	public void start(){
		nodes = initializeNodes( node_count );
		for ( int i = 0 ; i < node_count ; i++ )
			nodes[ i ].addDataListener( this );

		long tempStepCounter = 0;
		while ( isActive ) {
			if ( tempStepCounter <= step_counter || step_counter == -1 ) {
				tempStepCounter++;
				DataPacket dataPacket;
				synchronized ( dataBuffer ) {
					dataPacket = dataBuffer.remove( 0 );
				}

        String[] fields = {"NODE_ID", "PARENT_ID", "TEMPREATURE"};
        Serializable[] values = {dataPacket.getIdentifier(), dataPacket.getParent(), dataPacket.getValue()};
        long timestamp = System.currentTimeMillis();
        StreamElement streamElement = StreamElement.from(this);
        for (int i=0;i<fields.length;i++)
          streamElement.set(fields[i],values[i]);
        streamElement.setTime(timestamp);
        
				dataChannel.write( streamElement );
				if ( dataBuffer.size( ) > 0 ) continue;

			}
			try {
				Thread.sleep( rate );

			} catch ( InterruptedException e ) {
				logger.error( e.getMessage( ) , e );
			}

		}
		for ( WirelessNode node : nodes )
			node.stopNode( );
	}

	private static  final DataField[] dataField  = new DataField[] {new DataField( "NODE_ID" , DataTypes.INTEGER_NAME  ) ,
		new DataField( "PARENT_ID" , DataTypes.INTEGER_NAME  ) ,
		new DataField( "TEMPREATURE" , DataTypes.INTEGER_NAME)};

	public DataField [] getOutputFormat ( ) {
		return dataField;
	}

	public static int randomNumber ( int fromNo , int toNo ) {
		return ( int ) ( ( Math.random( ) * ( toNo - fromNo + 1 ) ) + fromNo );
	}

	public WirelessNode [ ] initializeNodes ( int nodeCount ) throws RuntimeException {
		if ( nodeCount <= 0 ) throw new RuntimeException( "Wireless Sensor Network Simulator (WSNS) can't create a network with zero or negative number of nodes : " + nodeCount );
		WirelessNode [ ] nodes = new WirelessNode [ nodeCount ];
		for ( int i = 0 ; i < nodeCount ; i++ ) {
			nodes[ i ] = new WirelessNode( i );
			nodes[ i ].setName( "WSNS-Node-" + i );
		}
		for ( int i = 1 ; i < nodeCount ; i++ )
			nodes[ i ].setParent( nodes[ randomNumber( i - 1 , 0 ) ] );
		for ( int i = 1 ; i < nodeCount ; i++ )
			nodes[ i ].start( );
		return nodes;
	}

	public void newDataAvailable ( DataPacket dataPacket ) {
		synchronized ( dataBuffer ) {
			dataBuffer.add( dataPacket );
		}
	}

	private boolean isActive=true;

	public void dispose ( ) {

	}

	public void stop() {
		isActive = false;
	}
}
