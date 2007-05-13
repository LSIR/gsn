package gsn.gui.beans;

import gsn.beans.DataField;

import com.jgoodies.binding.beans.Model;

public class DataFieldModel extends Model {
	public static final String PROPERTY_DESCRIPTION = "description";

	public static final String PROPERTY_NAME = "name";

	public static final String PROPERTY_TYPE = "type";

	private String description;

	private String name;

	private String type;
	
	public DataFieldModel(){
		description = "Not Provided";
	}
	
	public DataFieldModel(DataField dataField){
		description = dataField.getDescription();
		name = dataField.getName();
		type = dataField.getType();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		String oldDescription = getDescription();
		this.description = description;
		firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, description);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = getName();
		this.name = name;
		firePropertyChange(PROPERTY_NAME, oldName, name);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		String oldType = getType();
		this.type = type;
		firePropertyChange(PROPERTY_TYPE, oldType, type);
	}
	
	
}
