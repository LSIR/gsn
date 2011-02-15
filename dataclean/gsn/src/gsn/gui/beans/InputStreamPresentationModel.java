package gsn.gui.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.util.ValidationUtils;

@SuppressWarnings("serial")
public class InputStreamPresentationModel extends PresentationModel {
	
	private final ValidationResultModel validationResultModel;
	private PropertyValidationSupport support;
	
	public InputStreamPresentationModel(InputStreamModel inputStreamModel) {
		super(inputStreamModel);
		validationResultModel = new DefaultValidationResultModel();
		support = new PropertyValidationSupport(inputStreamModel, "InputStream");
		initEventHandling();
		updateValidationResult();
	}
	
	public ValidationResultModel getValidationResultModel() {
        return validationResultModel;
    }
	
	private void initEventHandling() {
        PropertyChangeListener handler = new ValidationUpdateHandler();
        getBufferedModel(InputStreamModel.PROPERTY_INPUT_STREAM_NAME).addValueChangeListener(handler);
        getBufferedModel(InputStreamModel.PROPERTY_QUERY).addValueChangeListener(handler);
    }
	
	private void updateValidationResult() {
        ValidationResult result = validate();
        validationResultModel.setResult(result);
    }
	
	public ValidationResult validate() {
		support.clearResult();
		if(null == getBufferedValue(InputStreamModel.PROPERTY_INPUT_STREAM_NAME) || ValidationUtils.isBlank(getBufferedValue(InputStreamModel.PROPERTY_INPUT_STREAM_NAME).toString()))
			support.addError("Name", "should not be empty");
		if(null == getBufferedValue(InputStreamModel.PROPERTY_QUERY) || ValidationUtils.isBlank(getBufferedValue(InputStreamModel.PROPERTY_QUERY).toString()))
			support.addError("Query", "should not be empty");
		
		return support.getResult();
	}
	
	 private final class ValidationUpdateHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			updateValidationResult();
		}

	}
}
