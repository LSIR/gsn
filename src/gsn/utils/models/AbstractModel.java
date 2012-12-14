package gsn.utils.models;

import gsn.beans.DataField;
import gsn.beans.StreamElement;


public abstract class AbstractModel {
	
	protected AbstractModel nextModel;
	protected DataField[] outputfield;

	public DataField[] getOutputFields() {
		return outputfield;
	}

	public void setOutputFields(DataField[] outputStructure) {
		outputfield = outputStructure;
		
	}

	public abstract StreamElement pushData(StreamElement streamElement);



	public abstract StreamElement[] query(StreamElement params);
	
	public AbstractModel getNextModel(){
		return nextModel;
	}
	
	public void setNextModel(AbstractModel am){
		nextModel = am;
	}

	public abstract void setParam(String k, String string);
	
}
