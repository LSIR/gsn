package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that choose the instances according to a boolean vector.
 * @author jeberle
 *
 */
public class FilterFromSelection extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6080185146245135909L;
	
	private boolean[] m_select = null;
	

	@Override
	public String globalInfo() {
		return "A filter that choose the instances according to a boolean vector.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {
		if (m_select == null || m_select.length != instances.numInstances()){
			return instances;
		}
		else{
			Instances output = new Instances(instances);
			for(int i=m_select.length-1;i>=0;i--){
				if(!m_select[i]){output.delete(i);}
			}
			return output;
		}
	}
	
	public boolean[] getM_select() {
		return m_select;
	}

	public void setM_select(boolean[] m_select) {
		this.m_select = m_select;
	}
}
