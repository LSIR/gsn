package gsn.gui.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.beans.PropertyConnector;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.value.ComponentValueModel;
import com.jgoodies.binding.value.ConverterFactory;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.util.ValidationUtils;

public class VSensorConfigPresentationModel extends PresentationModel {

	private final ValidationResultModel validationResultModel;

	private PropertyValidationSupport support;

	public VSensorConfigPresentationModel(VSensorConfigModel vSensorConfigModel) {
		super(vSensorConfigModel);
		validationResultModel = new DefaultValidationResultModel();
		support = new PropertyValidationSupport(vSensorConfigModel, "VSensorConfig");
		initEventHandling();
		updateValidationResult();
	}

	public ValidationResultModel getValidationResultModel() {
		return validationResultModel;
	}

	private void initEventHandling() {
		PropertyChangeListener handler = new ValidationUpdateHandler();
		getBufferedModel(VSensorConfigModel.PROPERTY_NAME).addValueChangeListener(handler);

		getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED).addValueChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				updateMaximumRate();
			}
		});

		PropertyConnector connector = new PropertyConnector(ConverterFactory
				.createBooleanNegator(getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED)), "value",
				getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE), ComponentValueModel.PROPERTYNAME_ENABLED);
	}

	public void updateValidationResult() {
		ValidationResult result = validate();
		validationResultModel.setResult(result);
	}

	public ValidationResult validate() {
		support.clearResult();
		if (null == getBufferedValue(VSensorConfigModel.PROPERTY_NAME)
				|| ValidationUtils.isBlank(getBufferedValue(VSensorConfigModel.PROPERTY_NAME).toString()))
			support.addError("Name", "should not be empty");

		VSensorConfigModel vSensorConfigModel = (VSensorConfigModel) getBean();
		ArrayListModel inputStreams = vSensorConfigModel.getInputStreams();
		if (inputStreams.getSize() == 0) {
			support.addError("InputStreams", "at least one input stream is needed");
		} else {
			for (Iterator iter = inputStreams.iterator(); iter.hasNext();) {
				InputStreamModel inputStreamModel = (InputStreamModel) iter.next();
				if (inputStreamModel.getSources().getSize() == 0)
					support.addError("StreamSources", "at least one stream source is needed for input stream : "
							+ inputStreamModel.getInputStreamName());
			}
		}
		return support.getResult();
	}

	private final class ValidationUpdateHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			updateValidationResult();
		}

	}

	protected void updateMaximumRate() {
		// boolean enabled =
		// getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED).booleanValue();
		// System.out.println(getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE));
		// getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE).setEnabled(enabled);
	}

}
