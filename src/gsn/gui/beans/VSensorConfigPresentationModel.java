package gsn.gui.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import org.apache.commons.validator.GenericValidator;

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

	private static final int UNLIMITED_OUTPUT_RATE = 0;

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
		getBufferedModel(VSensorConfigModel.PROPERTY_STORAGE_HISTORY_SIZE).addValueChangeListener(handler);

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
		
		String historySize = (String) getBufferedValue(VSensorConfigModel.PROPERTY_STORAGE_HISTORY_SIZE);
		if (!GenericValidator.isBlankOrNull(historySize)) {
			historySize = historySize.replace( " " , "" ).trim( ).toLowerCase( );
			final int mIndex = historySize.indexOf("m");
			final int hIndex = historySize.indexOf("h");
			final int sIndex = historySize.indexOf("s");
			if (mIndex < 0 && hIndex < 0 && sIndex < 0) {
				if (!GenericValidator.isInt(historySize))
					support.addError("History Size", "invalid history size");
			} else {
				final StringBuilder shs = new StringBuilder(historySize);
				int index = sIndex;
				if (hIndex >= 0)
					index = hIndex;
				else if (mIndex >= 0)
					index = mIndex;
				if (index != shs.length() - 1 || !GenericValidator.isInt(shs.deleteCharAt(index).toString()))
					support.addError("History Size", "invalid history size");
			}
		}else{
			setBufferedValue(VSensorConfigModel.PROPERTY_STORAGE_HISTORY_SIZE, null);
		}

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
		 boolean selected = getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED).booleanValue();
		 if(selected)
			 getModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE).setValue(UNLIMITED_OUTPUT_RATE);
		 System.out.println("selected : (" + selected + ") ,value = " + getModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE).getValue());
	}

}
