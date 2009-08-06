package gsn.operators;

import gsn.beans.DataField;
import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;

import java.util.List;

import org.apache.log4j.Logger;

public class SMACleaner  implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}

	
	static int index = 0;
	static double values[] ;
	static private double error_threshold;
	
	private static final transient Logger logger = Logger.getLogger(SensorscopeVS.class);
	private DataChannel outputChannel;
	
	public void process(String inputStreamName,StreamElement in) {
		Double input = (Double) in.getValue(in.getFieldNames()[0]);
		
		if (index>=values.length) {
			double sum = 0;
			for (double v:values)
				sum+=v;
			double sma = sum/values.length;
			
			boolean isAcceptable =  (Math.abs(input - sma)/input <= error_threshold );
			
      StreamElement se = StreamElement.from(this).setTime(in.getTimed()).set("raw_value",input).set("acceptable",(isAcceptable == false ? 0 : 1));
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
