package gsn.gui.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.util.ValidationUtils;

public class DataFieldPresentationModel extends PresentationModel {

	private final ValidationResultModel validationResultModel;
	private PropertyValidationSupport support;
	
	public DataFieldPresentationModel(DataFieldModel bean) {
		super(bean);
		validationResultModel = new DefaultValidationResultModel();
		support = new PropertyValidationSupport(bean, "DataField");
		initEventHandling();
		updateValidationResult();
	}
	
	public ValidationResultModel getValidationResultModel() {
        return validationResultModel;
    }
	
	private void initEventHandling() {
        PropertyChangeListener handler = new ValidationUpdateHandler();
        getBufferedModel(DataFieldModel.PROPERTY_NAME).addValueChangeListener(handler);
        getBufferedModel(DataFieldModel.PROPERTY_TYPE).addValueChangeListener(handler);
    }
	
	private void updateValidationResult() {
        ValidationResult result = validate();
        validationResultModel.setResult(result);
    }
	
	public ValidationResult validate() {
		support.clearResult();
		if(null == getBufferedValue(DataFieldModel.PROPERTY_NAME) || ValidationUtils.isBlank(getBufferedValue(DataFieldModel.PROPERTY_NAME).toString()))
			support.addError("Name", "should not be empty");
		if(null == getBufferedValue(DataFieldModel.PROPERTY_TYPE) || ValidationUtils.isBlank(getBufferedValue(DataFieldModel.PROPERTY_TYPE).toString()))
			support.addError("Type", "should not be empty");
		
		return support.getResult();
	}
	
	 private final class ValidationUpdateHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			updateValidationResult();
		}

	}
}
