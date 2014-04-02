/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
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
* File: src/gsn/utils/models/ModelExecuter.java
*
* @author Saket Sathe
* @author Sofiane Sarni
*
*/

package gsn.utils.models;
import gsn.utils.models.jgarch.armamodel.ARModel;
import gsn.utils.models.jgarch.garchmodel.GarchModel;
import gsn.utils.models.jgarch.util.ArrayUtils;
import gsn.utils.models.jgarch.wrappers.REngineManager;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

public class ModelExecuter {

	// Time series on which value and variance predictions are to be performed.
	private static double[] tseries;

	private static int windowSize=100;

	public static void main(String[] args) throws FileNotFoundException, IOException {

		List<Double> ts = new ArrayList<Double>();

		// Read time series from a file
		try {

		CSVReader reader = new CSVReader(new FileReader("/tmp/nyse.dat"),' ');
	    String [] nextLine;
	    while ((nextLine = reader.readNext()) != null) {
	        // nextLine[] is an array of values from the line
	    	for (String s: nextLine){
	    	System.out.println(s);
	    		ts.add(Double.parseDouble(s));
	    	}
	    }

		} catch (FileNotFoundException e){
			System.out.println("Exception caused:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("Exception caused:" + e.getMessage());
		}

		// Initialize holders arrays to hold the predicted value, predicted +ve variance (sigma^2_t)
		// and predicted -ve variance (-sigma^2_t)
		double [] predUVar = new double[ts.size()+1];
		double [] predLVar = new double[ts.size()+1];
		double [] predValue = new double[ts.size()+1];

		// will them with NaNs until the windowSize is reached
		for (int i=0; i < windowSize; i++) {

			predUVar[i] = Double.NaN;
			predLVar[i] = Double.NaN;
			predValue[i] = Double.NaN;

		}

		// Sliding Window
		for (int i = windowSize -1;i < ts.size();i++){
			List<Double> tsW = ts.subList(i-windowSize+1, i);
			Object[] ts1 = tsW.toArray();

			// window of readings
			tseries = ArrayUtils.objArrayToDoubleArray(ts1);

			// create and execute AR model
			ARModel ar = new ARModel(tseries);
			ar.run();
			// predict next value from AR model
			double[] arPred = ar.getArPreds();
			predValue[i+1] = arPred[0];

			// Get residuals from AR model and give them to GARCH model
			double[] arResid = ar.getArResiduals();
			GarchModel gm = new GarchModel(arResid);
			gm.run();

			// Predict +ve and -ve variance from GARCH model.
			predUVar[i+1] = gm.getPredUVar();
			predLVar[i+1] = gm.getPredLVar();

			System.out.println(gm.getPredUVar());
			System.out.println(gm.getPredLVar());
		}


		REngineManager rengineManager = REngineManager.getInstance();
    	rengineManager.endEngine();

	}


}
