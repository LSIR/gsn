package weka.filters.unsupervised.instance;

import java.util.Random;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that resample the instances randomly, given a certain ratio.
 * @author jeberle
 *
 */
public class RandomSample extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	private static final long serialVersionUID = -6080185146245135909L;
	
	
	private int m_ratio = 1;

	@Override
	public String globalInfo() {
		return "A filter that resample the instances randomly, given a certain ratio.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {
		
		Instances output = new Instances(instances);

		if(instances.numInstances() <= 2*m_ratio){return output;}
		
		Random r = new Random();
		
		for(int i=output.numInstances()-1;i>=0;i--){
			if(output.numInstances()>2 && r.nextInt(m_ratio) != 0){output.delete(i);}
		}		
		
		return output;
	}

	public int getM_ratio() {
		return m_ratio;
	}

	public void setM_ratio(int m_ratio) {
		this.m_ratio = m_ratio;
	}
	
}
