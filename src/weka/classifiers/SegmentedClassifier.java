package weka.classifiers;

import java.util.Arrays;

import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.ErrorBased;
import weka.filters.unsupervised.instance.SubsetByExpression;

/**
 * A classifier built using several sub-classifier, each one taking care of a segment.
 * The segments are defined by the list of cutting points.
 * It includes the option of filtering the data on each segment to downsample.
 * @author jeberle
 *
 */
public class SegmentedClassifier extends Classifier{

	private static final long serialVersionUID = 2311122072643482718L;
	
	private int segmentedClass = -1;
	
	private Double[] segments ;
	
	private Classifier[] classifiers;
	
	private Filter filter ;

	/**
	 * Initialize a new Classifier
	 * @param c the model of the sub-classifier
	 * @param segClass the feature to segment
	 * @param seg2 the list of cutting points
	 * @param filter the filter to downsample the data
	 * @throws Exception
	 */
	public SegmentedClassifier(Classifier c, int segClass,Double[] seg2,Filter filter) throws Exception {
		this.filter = filter;
		segmentedClass = segClass;
		segments = seg2;
		classifiers = Classifier.makeCopies(c, numSegments());
	}
	
	/**
	 * how many segments do we have?
	 * @return the number of segments
	 */
	private int numSegments(){
		return segments.length + 1;
	}
	
	/**
	 * Get the instances from the dataset, belonging to the given segment and apply the downsampling
	 * @param is the dataset
	 * @param idx the index of the segment to extract
	 * @return the instances in this segment or null if the segment doesn't exist
	 * @throws Exception
	 */
	public Instances getSegment(Instances is, int idx) throws Exception{
		if (idx >= numSegments() || idx < 0 || segmentedClass <= 0 || segmentedClass > is.numAttributes()){
			return null;
		}else{
			
			Filter f = Filter.makeCopy(filter);
			
			if (numSegments() == 1){
				//System.out.println("size before:"+is.numInstances());
				Instances ret = Filter.useFilter(is, f);
				//System.out.println("size after:"+ret.numInstances());
				return ret;}
			SubsetByExpression sbe = new SubsetByExpression();
			sbe.setInputFormat(is);
			String expr = "";
			if (idx == 0){
				expr += "(ATT"+segmentedClass+" < "+segments[idx]+")";
				sbe.setExpression(expr);
				Instances t = Filter.useFilter(is, Filter.makeCopy(sbe));
				if(f instanceof ErrorBased ){
					double[] e = Arrays.copyOfRange(((ErrorBased)f).getM_errors(),0,t.numInstances());
					((ErrorBased) f).setM_errors(e);
				}
			}else if(idx == numSegments()-1){
				expr += "(ATT"+segmentedClass+" >= "+segments[idx-1]+")";
				sbe.setExpression(expr);
				Instances t = Filter.useFilter(is, Filter.makeCopy(sbe));
				if(f instanceof ErrorBased ){
					double[] e = Arrays.copyOfRange(((ErrorBased)f).getM_errors(),((ErrorBased)f).getM_errors().length-t.numInstances(),((ErrorBased)f).getM_errors().length);
					((ErrorBased) f).setM_errors(e);
				}
			}else{
				String expr1 = "(ATT"+segmentedClass+" < "+segments[idx-1]+")";
				SubsetByExpression sbe1 = new SubsetByExpression();
				sbe1.setInputFormat(is);
				sbe1.setExpression(expr1);
				Instances t = Filter.useFilter(is, sbe1);
				expr += "(ATT"+segmentedClass+" >= "+segments[idx-1]+") and (ATT"+segmentedClass+" < "+segments[idx]+")";
				sbe.setExpression(expr);
				Instances tt = Filter.useFilter(is, Filter.makeCopy(sbe));
				if(f instanceof ErrorBased ){
					double[] e = Arrays.copyOfRange(((ErrorBased)f).getM_errors(),t.numInstances(),t.numInstances()+tt.numInstances());
					((ErrorBased) f).setM_errors(e);
				}
			}
			sbe.setExpression(expr);
			Instances t = Filter.useFilter(is, sbe);
			//System.out.println("size before:"+t.numInstances());
			Instances ret = Filter.useFilter(t, f);
			//System.out.println("size after:"+ret.numInstances());
			return ret;
		}
	}
	
	/**
	 * get the index of the segment corresponding to the given instance
	 * @param i the instance
	 * @return
	 */
	private int getSegmentNum(Instance i){
		double value = i.value(segmentedClass-1);
		int idx = 0;
		while (idx < segments.length && segments[idx] < value){
			idx++;
		}
		return idx;
	}
	

	@Override
	public void buildClassifier(Instances data) throws Exception {
		for (int i =0;i<classifiers.length;i++) {
			classifiers[i].buildClassifier(getSegment(data,i));
		}
	}
	
	@Override
	public double classifyInstance(Instance i){
		try {
			return classifiers[getSegmentNum(i)].classifyInstance(i);
		} catch (Exception e) {
			return 0;
		}
	}
}
