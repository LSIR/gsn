package weka.filters.unsupervised.instance;


import java.util.Arrays;

import java.util.Random;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that resample the instances according to their errors.
 * The error array must have the same size (and no offset) than the instances !!
 * @author jeberle
 *
 */
public class ErrorBased extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	private static final long serialVersionUID = -6080185146245135909L;
	
	private double[] m_errors ;
	
	private int m_ratio = 100;

	@Override
	public String globalInfo() {
		return "A filter that resample the instances according to their errors.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {
		
		Instances output = new Instances(instances);
		
		double[] dif = m_errors.clone();
		Arrays.sort(dif);
		double quantil = dif[(int)(dif.length*(1-1.0/m_ratio))];
		int after = (int)(dif.length*(1-1.0/m_ratio));
		int middle = (int)(dif.length*(1-1.0/m_ratio));
		int before = (int)(dif.length*(1-1.0/m_ratio));
		while (after < dif.length && dif[after] == quantil){after++;}
		while (before >= 0 && dif[before] == quantil){before--;}
		Random r = new Random();
		if(instances.numInstances() <= m_ratio){return output;}

		for(int i=output.numInstances()-1;i>=0;i--){
			if(output.numInstances() <= m_ratio){break;}
			if(m_errors.length > i && m_errors[i] < quantil){output.delete(i);}
			if(m_errors.length > i && m_errors[i]==quantil && r.nextInt(after-before)>middle-before){
				output.delete(i);
			}
			
			

		}		
		
		return output;
	}

	public double[] getM_errors() {
		return m_errors;
	}

	public void setM_errors(double[] m_errors) {
		this.m_errors = m_errors;
	}
	public int getM_ratio() {
		return m_ratio;
	}

	public void setM_ratio(int m_ratio) {
		this.m_ratio = m_ratio;
	}
	
}
