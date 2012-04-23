package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that resample the instances uniformly.
 * @author jeberle
 *
 */
public class SubSample extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	private static final long serialVersionUID = -6080185146245135909L;
	
	private int m_index = 0;
	
	private int m_ratio = 1;

	@Override
	public String globalInfo() {
		return "A filter that resample the instances uniformly.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {

		instances.sort(m_index);
		
		Instances output = new Instances(instances);
		
		if(instances.numInstances() <= m_ratio){return output;}
		
		for(int i=output.numInstances()-1;i>=0;i--){
			if((i+1) % m_ratio != 0){output.delete(i);}
		}
		//output.compactify();
		
		
		return output;
	}

	public int getM_index() {
		return m_index;
	}

	public void setM_index(int m_index) {
		this.m_index = m_index;
	}

	public int getRatio() {
		return m_ratio;
	}

	public void setRatio(int ratio) {
		this.m_ratio = ratio;
	}

}
