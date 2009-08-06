package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

public class StreamRRDExporterVirtualSensor implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}

	public static final String            PARAM_RRDFILE    = "rrdfile" ;

	public static final String            PARAM_FIELD    = "field" ;

	private static final transient Logger logger        = Logger.getLogger(StreamRRDExporterVirtualSensor.class );

	private String rrdfile = null;

	private String[] fields ;

	private DataChannel outputChannel;
	
	public StreamRRDExporterVirtualSensor (OperatorConfig config,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
		rrdfile = config.getParameters().getPredicateValueWithException(PARAM_RRDFILE);
		logger.debug( "rrdfile=" + this.rrdfile);
		if (!ensureFileExistence(rrdfile)) 
			createRRDFile();
		this.fields = config.getParameters().getPredicateValues(PARAM_FIELD);
	}

	private boolean createRRDFile() {
		String command = "rrdtool create "+rrdfile+" --step 300 ";
		for(int i=0;i<fields.length;i++){
			command = command + "DS:field"+i+":GAUGE:600:0:U ";
		}
		command = command + "RRA:AVERAGE:0.5:1:600 ";
		command = command + "RRA:AVERAGE:0.5:6:700 ";
		command = command + "RRA:AVERAGE:0.5:24:775 ";
		command = command + "RRA:AVERAGE:0.5:288:797 ";
		command = command + "RRA:MAX:0.5:1:600 ";
		command = command + "RRA:MAX:0.5:6:700 ";
		command = command + "RRA:MAX:0.5:24:775";
		command = command + "RRA:MAX:0.5:288:797";
		Runtime runtime = Runtime.getRuntime();
		try {
			if(logger.isDebugEnabled())
				logger.debug( "The used rrdtool create command is: " + command);                        
			Process process = runtime.exec(command);
			if(logger.isDebugEnabled())
				logger.debug( "The exit value of the rrdtool create command is: " +
						process.exitValue());
			return true;
		} catch (IOException e) {
			logger.debug("An IOException has occured: "+e);
			return false;
		} 
	}


	public void process ( String inputStreamName , StreamElement streamElement ) {
		ensureFileExistence( );
		exportValues( streamElement );
	}


	/*
     returns true if the requested file exists.
	 * @param filename The file name to check for.
	 */
	private boolean ensureFileExistence ( ) {
		return ensureFileExistence(this.rrdfile);
	}

	/*
     returns true if the requested file exists.
	 * @param filename The file name to check for.
	 */
	private boolean ensureFileExistence ( String filename ) {
		File file = new File(rrdfile);
		if (file.exists()) return true;
		else {
			logger.error("rrdfile "+rrdfile+" does not exist!");
			return false;
		}
	}


	/*
	 * Export all received values from a stream to the proposed table name into
	 * the database selected by the currently open connection.
	 */

	private void exportValues ( StreamElement streamElement ) {
		if(logger.isDebugEnabled())
			logger.debug( "Trying to add new data items to the rrdfile:" + this.rrdfile );
		String command ="rrdtool update "+rrdfile+" N";
			String field;

		for(int i=0;i<streamElement.getFieldNames().length;i++){
			field = streamElement.getValue(streamElement.getFieldNames()[i]).toString();
			// if the field is empty we have to add an U for unknown to the rrdfile
			if(field==null || field.equals("")) field = "U";
			command = command+":"+field;
		}

		Runtime runtime = Runtime.getRuntime();
		try {
			if(logger.isDebugEnabled())
				logger.debug( "The used rrdtool update command is: " + command);                        
			Process process = runtime.exec(command);
			if(logger.isDebugEnabled())
				logger.debug( "The processing did not generate an error!");                        
		} catch (IOException e) {
			logger.debug("An IOException has occured: "+e);
		} 
	}

	public void dispose ( ) {
	}

}
