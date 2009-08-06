package gsn.operators;

import gsn.beans.DataField;
import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

public class SMACleaner  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

	public void start() {}
	public void stop() {}

	
	static int index = 0;
	static double values[] ;
	static private double error_threshold;
	
	private static final transient Logger logger = Logger.getLogger(SensorscopeVS.class);
	private DataChannel outputChannel;
	
	public void process(String inputStreamName,StreamElement in) {
		Double input = (Double) in.getData()[0];
		
		if (index>=values.length) {
			double sum = 0;
			for (double v:values)
				sum+=v;
			double sma = sum/values.length;
			
			StreamElement se ;
			boolean isAcceptable =  (Math.abs(input - sma)/input <= error_threshold );
			se= new StreamElement(
					new DataField[] {new DataField("raw_value","double" ), new DataField("acceptable","integer")},
					new Serializable[] {input,(isAcceptable == false ? 0 : 1)},
					in.getTimeStamp());
			outputChannel.write(se);
		}
		values[index++%values.length]= input;
	}

	public void dispose() {
		
	}

	public  SMACleaner(OperatorConfig config,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
		int size = config.getParameters().getPredicateValueAsIntWithException("size");
		error_threshold = config.getParameters().getPredicateValueAsDoubleWithException("error-threshold");
		values = new double[size];
	}

}
