package gsn.operators;

import gsn.beans.DataTypes;
import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.channels.DataChannel;
import gsn.channels.GSNChannel;
import gsn.utils.MatlabEngine;
import gsn2.conf.OperatorConfig;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

public class MatlabVS implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

	public void start() {}
	public void stop() {}


	private final static transient Logger      logger         = Logger.getLogger( GSNChannel.class );
	private MatlabEngine engine;


	private String[] fieldNames = {"Matlab_Result"};
	private Byte[] fieldTypes = {DataTypes.DOUBLE};

	private String functionName;
	private final String defaultFunctionName = "myGSNMatlabFunction";
	private Integer nbArgs;
	private Double[] parameters;
	private DataChannel outputChannel;
	 
	public void process(String inputStreamName,StreamElement streamElement) {
		if(streamElement.getFieldTypes().length == nbArgs+1)
			for(int i = 0; i < nbArgs; i++)
				parameters[i] = (Double) streamElement.getData()[i];
		Double answer;
		try {
			String matlabCommand = functionName + "(" ;
			for(int i = 0; i < nbArgs; i++) {
				matlabCommand = matlabCommand + parameters[i].toString();
				if(i != nbArgs-1)
					matlabCommand = matlabCommand +",";
			}
			if(nbArgs > 0)
				matlabCommand = matlabCommand + ")";
			if(logger.isDebugEnabled())
				logger.debug("Calling matlab engine with command: " + matlabCommand);
			engine.evalString(matlabCommand);
			String matlabAnswer = engine.getOutputString(100);
			if(logger.isDebugEnabled())
				logger.debug("Received output from matlab: " + matlabAnswer +". Trying to interpret this"
						+ " answer as a Java Float object.");
			answer = Double.parseDouble(matlabAnswer);
			StreamElement result = new StreamElement(fieldNames, fieldTypes , new Serializable[] {answer});
			outputChannel.write(result);
		} catch (IOException e) {
			logger.warn(e);
		}


	}

	public void dispose() {
		try {
			engine.close();
		} catch (InterruptedException e) {
			logger.warn(e);
		} catch (IOException e) {
			logger.warn(e);
		}
	}

	public MatlabVS(OperatorConfig config,DataChannel outputChannel ) throws IOException {
		this.outputChannel = outputChannel;
		String functionName = config.getParameters().getPredicateValueWithDefault("function",defaultFunctionName);
		
		nbArgs = config.getParameters().getPredicateValueAsInt("arguments",0);
		parameters = new Double[nbArgs];
		
		
		engine = new MatlabEngine();
		// Matlab start command:
		engine.open("matlab -nosplash -nojvm");
		// Display output:
		if(logger.isDebugEnabled())
			logger.debug(engine.getOutputString(500));
		
		if(logger.isDebugEnabled())
			logger.debug("Function name configured to: " + functionName);
		
		if(logger.isDebugEnabled())
			logger.debug("Number of arguments configured to: " + nbArgs);
		
	}

	
}
