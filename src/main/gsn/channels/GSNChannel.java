package gsn.channels;

import gsn.ContainerImpl;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSFile;
import gsn2.conf.OperatorConfig;

import java.io.Serializable;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class GSNChannel implements DataChannel{

	private static final transient Logger logger           = Logger.getLogger( GSNChannel.class );

	private void validateStreamElement ( StreamElement streamElement ,boolean adjust) {
//		if ( !compatibleStructure( streamElement, getVirtualSensorConfiguration( ).getProcessingClassConfig().getOutputFormat(),adjust ) ) {
//			StringBuilder exceptionMessage = new StringBuilder( ).append( "The streamElement produced by :" ).append( getVirtualSensorConfiguration( ).getName( ) ).append(
//			" Virtual Sensor is not compatible with the defined streamElement.\n" );
//			exceptionMessage.append( "The expected stream element structure (specified in " ).append( getVirtualSensorConfiguration( ).getFileName( ) ).append( " is [" );
//			exceptionMessage.append( "] but the actual stream element received from the " + getVirtualSensorConfiguration( ).getName( ) ).append( " has the [" );
//			for ( int i = 0 ; i < streamElement.getFieldNames( ).length ; i++ )
//				exceptionMessage.append( streamElement.getFieldNames( )[ i ] ).append( "(" ).append( DataTypes.TYPE_NAMES[ streamElement.getFieldTypes( )[ i ] ] ).append( ")," );
//			exceptionMessage.append(" ] thus the stream element dropped !!!" );
//			throw new RuntimeException( exceptionMessage.toString( ) );
//		}
	}
	/**
	 * if Adjust is true then system checks the output structure of the virtual sensor and
	 * only publishes the fields defined in the output structure of the virtual sensor and 
	 * ignores the rest. IF the adjust is set to false, the system will enforce strict
	 * compatibility of the output and the produced value.
	 * 
	 * @param streamElement
	 * @param adjust Default is false.
	 */
	protected synchronized void dataProduced ( StreamElement streamElement,boolean adjust ) {
		try {
			validateStreamElement( streamElement,adjust );
		} catch ( Exception e ) {
			logger.error( e.getMessage( ) , e );
			return;
		}
		if ( !streamElement.isTimestampSet( ) ) streamElement.setTime( System.currentTimeMillis( ) );

		try {
			ContainerImpl.getInstance().publishData( this ,streamElement);
		} catch (SQLException e) {
			if (e.getMessage().toLowerCase().contains("duplicate entry"))
				logger.info(e.getMessage(),e);
			else
				logger.error(e.getMessage(),e);
		}
	}
	/**
	 * Calls the dataProduced with adjust = false.
	 * @param streamElement
	 */
	public synchronized void write( StreamElement streamElement ) {
		dataProduced(streamElement,true);
	}
	/**
	 * First checks compatibility of the data type of each output data item in the stream element with the
	 * defined output in the VSD file. (this check is done regardless of the value for adjust flag).
	 * <p>
	 * If the adjust flag is set to true, the method checks the newly generated stream element
	 * and returns true if and only if the number of data items is equal to the number of output
	 * data structure defined for this virtual sensor.
	 * If the adjust=true, then this test is not performed.
	 * 
	 * @param se
	 * @param outputStructure
	 * @param adjust default is false.
	 * @return
	 */
	private static boolean compatibleStructure ( StreamElement se ,  DataField [] outputStructure ,boolean adjust ) {
		if (!adjust && outputStructure.length != se.getFieldNames().length ) {
			logger.warn( "Validation problem, the number of field doesn't match the number of output data strcture of the virtual sensor" );
			return false;
		}
		int i =-1;
		for (DataField field: outputStructure) {
			Serializable value = se.getValue(field.getName());
			i++;
			if (value==null)
				continue;
			if ( ( (  field.getDataTypeID() == DataTypes.BIGINT ||
					field.getDataTypeID() == DataTypes.DOUBLE ||
					field.getDataTypeID() == DataTypes.INTEGER||
					field.getDataTypeID() == DataTypes.SMALLINT||
					field.getDataTypeID() == DataTypes.TINYINT ) &&!(value instanceof Number)) 
					||
					( (field.getDataTypeID() == DataTypes.VARCHAR || field.getDataTypeID() == DataTypes.CHAR) && !(value instanceof String)) ||
					( (field.getDataTypeID() == DataTypes.BINARY) && !(value instanceof byte[])) 
			){ 
				logger.warn( "Validation problem for output field >" + field.getName( ) + ", The field type declared as >" + field.getType()+"< while in VSD it is defined as >"+DataTypes.TYPE_NAMES[outputStructure[ i ].getDataTypeID( )]);
				return false;
			}
		}
		return true;
	}

	private VSFile                 virtualSensorConfiguration;
	
	/**
	 * @return the virtualSensorConfiguration
	 */
	public VSFile getVirtualSensorConfiguration ( ) {
		if ( virtualSensorConfiguration == null ) { throw new RuntimeException( "The VirtualSensorParameter is not set !!!" ); }
		return virtualSensorConfiguration;
	}

	/**
	 * @param virtualSensorConfiguration the virtualSensorConfiguration to set
	 */
	public void setVirtualSensorConfiguration ( VSFile virtualSensorConfiguration ) {
		this.virtualSensorConfiguration = virtualSensorConfiguration;
	}
	
	private OperatorConfig config;

	public GSNChannel(OperatorConfig config) {
		this.config= config;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()	{
	    final String TAB = "    ";
	    
	    String retValue = "";
	    
	    retValue = "GSNChannel ( "
	        + super.toString() + TAB
	        + "virtualSensorConfiguration = " + this.virtualSensorConfiguration + TAB
	        + "config = " + this.config + TAB
	        + " )";
	
	    return retValue;
	}
	
	
	
}
