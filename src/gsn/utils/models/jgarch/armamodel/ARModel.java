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
* File: src/gsn/utils/models/jgarch/armamodel/ARModel.java
*
* @author Saket Sathe
* @author Sofiane Sarni
*
*/

package gsn.utils.models.jgarch.armamodel;

import gsn.utils.models.jgarch.wrappers.REngineManager;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

public class ARModel {
	private double[] arResiduals;
	
	private double[] tSeries;
	
	private int arOrder = 2;
	
	private double[] arPreds;
	
	private int predStep = 1;
	
	private int initialOffset = arOrder + 1;	
	
	
	public double[] getArPreds() {
		return arPreds;
	}	

	public void setPredStep(int predStep) {
		this.predStep = predStep;
	}
	
	

	public double[] getArResiduals() {
		return arResiduals;
	}

	public ARModel(double[] tSeries, int arOrder, int predStep){
		this.tSeries = tSeries;		
		this.arOrder = arOrder;
		this.initialOffset = arOrder+1;
		this.predStep = predStep;
		
	}
	
	public ARModel(double[] tSeries){
		this.tSeries = tSeries;		
	}
	
    public void run() {
    	
    	REngineManager rengineManager = REngineManager.getInstance();
    	Rengine re = rengineManager.getREngine();	
		
		try {						
			
			REXP RarResiduals;
			REXP RarPreds;
			re.assign("valseries", tSeries);
						
			re.eval("valseries.ar=ar.mle(valseries, aic=FALSE, order.max="+ arOrder + ")");
			RarResiduals=re.eval("valseries.ar$resid["+initialOffset + ":length(valseries)]");
			RarResiduals=re.eval("valseries.ar$resid["+initialOffset + ":length(valseries)]");
			
			this.arResiduals = RarResiduals.asDoubleArray();
			re.eval("valpred=predict(valseries.ar,n.ahead="+predStep+")");
			RarPreds=re.eval("valpred$pred");
			
			this.arPreds = RarPreds.asDoubleArray(); 
			
		} catch (Exception e) {
			System.out.println("EX:"+e);
			e.printStackTrace();
			rengineManager.endEngine();
		} 
	
    }
}
