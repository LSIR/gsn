package tinygsn.services;

import tinygsn.beans.DataField;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.wrappers.AbstractWrapper;


public abstract class AbstractScheduler extends AbstractWrapper {

	public final static String[] SCHEDULER_LIST = new String[]{"tinygsn.services.LocationScheduler"};
	
	public AbstractScheduler(WrapperConfig wc) {
		super(wc);
	}
	
	public AbstractScheduler(){	}

	public abstract String[] getManagedSensors();
	
	@Override
	public DataField[] getOutputStructure() {
		return null;
	}

	@Override
	public String[] getFieldList() {
		return null;
	}

	@Override
	public Byte[] getFieldType() {
		return null;
	}
	

}
