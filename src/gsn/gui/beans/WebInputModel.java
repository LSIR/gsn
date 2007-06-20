package gsn.gui.beans;

import gsn.beans.DataField;
import gsn.beans.WebInput;
import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.list.ArrayListModel;

public class WebInputModel extends Model {
	public static final String PROPERTY_NAME = "name";
	
	private String name;
	private ArrayListModel parameters;
	
	public WebInputModel(){
		parameters = new ArrayListModel();
	}
	
	public WebInputModel(WebInput webInput){
		name = webInput.getName();
		parameters = new ArrayListModel();
		addParameterList(webInput.getParameters());
	}

	private void addParameterList(DataField[] dataFields) {
		for (int i = 0; i < dataFields.length; i++) {
			addDataFieldModel(new DataFieldModel(dataFields[i]));
		}
	}
	
	public void addDataFieldModel(DataFieldModel dataFieldModel){
		parameters.add(dataFieldModel);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = getName();
		this.name = name;
		firePropertyChange(PROPERTY_NAME, oldName, name);
	}

	public ArrayListModel getParameters() {
		return parameters;
	}

	public void setParameters(ArrayListModel parameters) {
		this.parameters = parameters;
	}

	public WebInput getWebInput() {
		WebInput webInput = new WebInput();
		webInput.setName(getName());
		DataField[] dataFields = new DataField[getParameters().size()];
		for (int i = 0; i < getParameters().size(); i++) {
			dataFields[i] = ((DataFieldModel)getParameters().get(i)).getDataField();
		}
		webInput.setParameters(dataFields);
		return webInput;
	}
	
	
}
