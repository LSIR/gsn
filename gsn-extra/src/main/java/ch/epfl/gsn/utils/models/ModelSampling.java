/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/utils/models/ModelSampling.java
*
* @author Alexandru Arion
* @author Sofiane Sarni
*
*/

package ch.epfl.gsn.utils.models;

import ch.epfl.gsn.utils.models.helper.Segmenter;
import ch.epfl.gsn.utils.models.helper.Tools;
import weka.classifiers.Classifier;
import weka.classifiers.SegmentedClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.DummyFilter;
import weka.filters.unsupervised.instance.ErrorBased;
import weka.filters.unsupervised.instance.RandomSample;
import weka.filters.unsupervised.instance.SubSample;

public class ModelSampling {
	
	public static final int BINARY = 0;
	public static final int BINARY_PLUS = 1;
	public static final int HEURISTIC = 2;
	public static final int HEURISTIC_PLUS = 3;
	public static final int UNIFORM = 0;
	public static final int ERROR_BASED = 1;
	public static final int RANDOM = 2;

    final public static String SEGMENT_NAMES[] = {"BINARY","BINARY_PLUS","HEURISTIC","HEURISTIC_PLUS"};
	final public static String SAMPLING_NAMES[] = {"UNIFORM","ERROR_BASED","RANDOM"};
    final public static String MODEL_NAMES[] = {"SVM","LINEAR"};
    
    private int seg_method = 0;
	private int samp_method = 0;
	private int model = 0;
	private int seg_num = 1;
	private int samp_ratio = 1;
	private Classifier classifier = null;

    public ModelSampling(int model, int segment_method, int segment_num,
			int sampling_method, int sampling_ratio) {
    	seg_method = segment_method;
    	samp_method = sampling_method;
    	this.model = model;
    	seg_num = segment_num;
    	samp_ratio = sampling_ratio;
	}

	/*
    * Returns the id, given a string
    * comparison is case insensitive
    * */
    public static int getIdFromString(String[] array, String strModel) {

        int result = -1;

        if (strModel.matches("\\d")) {  // model given as number
            result = Integer.parseInt(strModel);
            return result;
        }

        for (int i = 0; i < array.length; i++) {
            if (array[i].toUpperCase().equals(strModel.toUpperCase())) {
                result = i;
                break;
            }
        }
        return result;
    }

	public Double predict(Instance i) {
		try{
		return new Double(classifier.classifyInstance(i));
		}catch(Exception e){
			return null;
		}
	}
	
	public int train(Instances training_set,int model, int segment_method, int segment_num,
			int sampling_method, int sampling_ratio) {
    	seg_method = segment_method;
    	samp_method = sampling_method;
    	this.model = model;
    	seg_num = segment_num;
    	samp_ratio = sampling_ratio;
		return train(training_set);
	}

	public int train(Instances training_set) {
		try{
			Segmenter s = new Segmenter(seg_method,model);
			Double[] seg = s.getSegments(seg_num,training_set);
			if(seg == null){return 0;}
			s.computeErrors(training_set, seg);
			Filter f = null;
			if(samp_method == UNIFORM){
				SubSample ss = new SubSample();
			    ss.setInputFormat(training_set);
			    ss.setRatio(samp_ratio);
			    ss.setM_index(0);
			    f=ss;
			}else if(samp_method == ERROR_BASED){
			    ErrorBased ss = new ErrorBased();
			    ss.setInputFormat(training_set);
			    ss.setM_ratio(samp_ratio);
			    ss.setM_errors(s.Pred_errors);
			    f=ss;
			}else if(samp_method == RANDOM)
				{
			    RandomSample ss = new RandomSample();
			    ss.setInputFormat(training_set);
			    ss.setM_ratio(samp_ratio);
			    f=ss;
			}else{
				DummyFilter ss = new DummyFilter();
				ss.setInputFormat(training_set);
				f = ss;
			}
			classifier = new SegmentedClassifier(Tools.getClassifierById(model), 1, seg,f);
			classifier.buildClassifier(training_set);
			return 1;
		}catch(Exception e){
			return 0;
		}
	}
}
