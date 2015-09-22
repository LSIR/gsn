package gsn.utils.models;

import java.io.Serializable;
import java.util.ArrayList;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gsn.Mappings;
import gsn.VirtualSensor;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.vsensor.AbstractVirtualSensor;
import gsn.vsensor.ModellingVirtualSensor;

public class AggregateModel extends AbstractModel {
	
	private static final transient Logger logger = LoggerFactory.getLogger(AggregateModel.class);

	private AbstractModel model_o3;
	private AbstractModel model_pm;
	private AbstractModel model_co;

	@Override
	public StreamElement[] pushData(StreamElement streamElement,String origin) {
		return new StreamElement[]{streamElement};
	}
	

	@Override
	public StreamElement[] query(StreamElement params) {
		StreamElement o = model_o3.query(params)[0];
		//StreamElement p = model_pm.query(params)[0];
		StreamElement c = model_co.query(params)[0];
		
		
		Serializable o3_rel = o.getData("O3_REL");
		Serializable co_rel = c.getData("CO_REL");
		//Serializable pm_rel = p.getData("PM_REL");
		
		Serializable o3_abs = o.getData("O3_ABS");
		Serializable co_abs = c.getData("CO_ABS");
		//Serializable pm_abs = p.getData("PM_ABS");
		
		if (o3_rel == null) o3_rel =-1;
		if (o3_abs == null) o3_abs =-1;
		if (co_rel == null) co_rel =-1;
		//if (pm_rel == null) pm_rel =-1;
		//if (pm_abs == null) pm_abs =-1;
		if (co_abs == null) co_abs =-1;
		
		Integer max_rel = Math.max((Integer)o3_rel,(Integer)co_rel);
		Integer max_abs = Math.max((Integer)o3_abs,(Integer)co_abs);
		//Integer max_rel = Math.max(Math.max((Integer)o3_rel,(Integer)pm_rel),(Integer)co_rel);
		//Integer max_abs = Math.max(Math.max((Integer)o3_abs,(Integer)pm_abs),(Integer)co_abs);


		return new StreamElement[]{new StreamElement(new DataField[]{new DataField("MAX_REL","INTEGER"),new DataField("MAX_ABS","INTEGER")},new Serializable[]{max_rel,max_abs})};
	}


	@Override
	public void setParam(String k, String string) {
		if (k.equalsIgnoreCase("inputsVS")){
			try{
				for(String n:string.split(",")){
					VirtualSensor vs = Mappings.getVSensorInstanceByVSName(n);
				    if (vs == null){
				    	logger.error("can't find VS: "+ n);
						//response.sendError(404);
				    }else{
					    AbstractVirtualSensor avs = vs.borrowVS();
						if (avs instanceof ModellingVirtualSensor){
							if(n.endsWith("o3")){
							model_o3 = ((ModellingVirtualSensor)avs).getModel(0);
							}else if(n.endsWith("co")){
								model_co = ((ModellingVirtualSensor)avs).getModel(0);
								}else if(n.endsWith("pm")){
									model_pm = ((ModellingVirtualSensor)avs).getModel(0);
								}
						}
						vs.returnVS(avs);
				    }
				}
			}catch(Exception e){
				
			}
		}
		
	}

}
