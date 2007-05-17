package gsn.gui.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.util.ValidationUtils;

public class StreamSourcePresentationModel extends PresentationModel {
	
	private final ValidationResultModel validationResultModel;
	private PropertyValidationSupport support;
	
	public StreamSourcePresentationModel(StreamSourceModel streamSourceModel){
		super(streamSourceModel);
		validationResultModel = new DefaultValidationResultModel();
		support = new PropertyValidationSupport(streamSourceModel, "StreamSource");
		initEventHandling();
		updateValidationResult();
	}
	
	public ValidationResultModel getValidationResultModel() {
        return validationResultModel;
    }
	
	private void initEventHandling() {
        PropertyChangeListener handler = new ValidationUpdateHandler();
        getBufferedModel(StreamSourceModel.PROPERTY_ALIAS).addValueChangeListener(handler);
        getBufferedModel(StreamSourceModel.PROPERTY_SQL_QUERY).addValueChangeListener(handler);
    }
	
	private void updateValidationResult() {
        ValidationResult result = validate();
        validationResultModel.setResult(result);
    }
	
	public ValidationResult validate() {
		support.clearResult();
		if(null == getBufferedValue(StreamSourceModel.PROPERTY_ALIAS) || ValidationUtils.isBlank(String.valueOf(getBufferedValue(StreamSourceModel.PROPERTY_ALIAS))))
			support.addError("Alias", "should not be empty");
		if(null == getBufferedValue(StreamSourceModel.PROPERTY_SQL_QUERY) || ValidationUtils.isBlank(String.valueOf(getBufferedValue(StreamSourceModel.PROPERTY_SQL_QUERY))))
			support.addError("SqlQuery", "should not be empty");
		
		return support.getResult();
	}
	
	 private final class ValidationUpdateHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			updateValidationResult();
		}

	}
}
