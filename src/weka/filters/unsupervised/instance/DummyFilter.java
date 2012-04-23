package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that does nothing
 * @author jeberle
 *
 */
public class DummyFilter extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6080185146245135909L;
	

	@Override
	public String globalInfo() {
		return "A filter that does nothing.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {

		return instances;
	}

}
