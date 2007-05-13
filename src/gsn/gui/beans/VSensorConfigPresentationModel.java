package gsn.gui.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.beans.PropertyConnector;
import com.jgoodies.binding.value.ComponentValueModel;
import com.jgoodies.binding.value.ConverterFactory;
import com.jgoodies.binding.value.ConverterFactory.BooleanNegator;

public class VSensorConfigPresentationModel extends PresentationModel {

	public VSensorConfigPresentationModel(VSensorConfigModel vSensorConfigModel) {
		super(vSensorConfigModel);
		initEventHandling();
	}

	private void initEventHandling() {
		getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED).addValueChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				updateMaximumRate();
			}
		});

		PropertyConnector connector = new PropertyConnector(ConverterFactory.createBooleanNegator(getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED)), "value",
				getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE), ComponentValueModel.PROPERTYNAME_ENABLED);
	}

	protected void updateMaximumRate() {
//		boolean enabled = getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED).booleanValue();
//		System.out.println(getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE));
//		getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE).setEnabled(enabled);
	}

}
