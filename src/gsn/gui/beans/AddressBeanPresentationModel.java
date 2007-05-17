package gsn.gui.beans;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.util.ValidationUtils;

public class AddressBeanPresentationModel extends PresentationModel {

	private final ValidationResultModel validationResultModel;
	private PropertyValidationSupport support;
	
	public AddressBeanPresentationModel(AddressBeanModel addressBeanModel) {
		super(addressBeanModel);
		validationResultModel = new DefaultValidationResultModel();
		support = new PropertyValidationSupport(addressBeanModel, "AddressBean");
		initEventHandling();
		updateValidationResult();
	}
	
	private void initEventHandling() {
        PropertyChangeListener handler = new ValidationUpdateHandler();
        getBufferedModel(AddressBeanModel.PROPERTY_WRAPPER).addValueChangeListener(handler);
    }
	
	
	private void updateValidationResult() {
        ValidationResult result = validate();
        validationResultModel.setResult(result);
    }
	
	public ValidationResult validate() {
		support.clearResult();
		if(null == getBufferedValue(AddressBeanModel.PROPERTY_WRAPPER) || ValidationUtils.isBlank(getBufferedValue(AddressBeanModel.PROPERTY_WRAPPER).toString()))
			support.addError("Wrapper", "should not be empty");
		return support.getResult();
	}
	
	public ValidationResultModel getValidationResultModel() {
        return validationResultModel;
    }
	
	private final class ValidationUpdateHandler implements PropertyChangeListener {
        
        public void propertyChange(PropertyChangeEvent evt) {
            updateValidationResult();
        }

	}
}
