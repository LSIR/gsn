package gsn.gui.forms;

import gsn.gui.beans.VSensorConfigModel;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class VSensorGeneralPanel {

	private PresentationModel vSensorConfigPresentationModel;
	private JTextField nameTextField;
	private JTextField priorityTextField;
	private JTextField generalPasswordTextField;
	private JTextField descriptionTextField;
	private JTextField poolSizeTextField;
	private JTextField storageSizeTextField;
	private JTextField maximumAllowedRateTextField;
	private JCheckBox marUnlimitedCheckBox;

	public VSensorGeneralPanel(PresentationModel presentationModel) {
		vSensorConfigPresentationModel = presentationModel;
	}

	public Component createPanel() {
		initComponents();
		KeyValueEditorPanel keyValueEditorPanel = new KeyValueEditorPanel(((VSensorConfigModel)vSensorConfigPresentationModel.getBean()).getAddressing());
		
		FormLayout layout = new FormLayout("pref:g", "pref, pref:g, pref, pref:g");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addSeparator("General properties", cc.xy(1, 1));
		builder.add(buildGeneralFieldsPanel(), cc.xy(1, 2));
		builder.addSeparator("Addressing", cc.xy(1, 3));
		builder.add(keyValueEditorPanel.createPanel(), cc.xy(1, 4));
		return builder.getPanel();
	}
	
	private Component buildGeneralFieldsPanel() {
		FormLayout layout = new FormLayout(
				"right:pref, 3dlu, pref:g, 7dlu, right:pref, 3dlu, pref:g, 3dlu, pref, 3dlu",
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
		layout.setColumnGroups(new int[][]{{1, 5}, {3, 7}});
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Name", cc.xy(1, 1));
		builder.add(nameTextField, cc.xy(3, 1));
		builder.addLabel("Priority", cc.xy(5, 1));
		builder.add(priorityTextField, cc.xyw(7, 1, 3));
		builder.addLabel("Password" , cc.xy(1, 3));
		builder.add(generalPasswordTextField, cc.xy(3, 3));
		builder.addLabel("Pool size", cc.xy(5, 3));
		builder.add(poolSizeTextField, cc.xyw(7, 3, 3));
		builder.addLabel("Storage size" , cc.xy(1, 5));
		builder.add(storageSizeTextField, cc.xy(3, 5));
		builder.addLabel("Maximum rate", cc.xy(5, 5));
		builder.add(maximumAllowedRateTextField, cc.xy(7, 5));
		builder.add(marUnlimitedCheckBox, cc.xy(9, 5));
		builder.addLabel("Description", cc.xy(1, 7));
		builder.add(descriptionTextField, cc.xyw(3, 7, 7));
		return builder.getPanel();
	}

	private void initComponents() {
		nameTextField = BasicComponentFactory.createTextField(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_NAME));
		priorityTextField = BasicComponentFactory.createIntegerField(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_PRIORITY));
		generalPasswordTextField = BasicComponentFactory.createTextField(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_GENERAL_PASSWORD));
		descriptionTextField = BasicComponentFactory.createTextField(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_DESCRIPTION));
		poolSizeTextField = BasicComponentFactory.createIntegerField(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_LIFECYCLE_POOL_SIZE));
		storageSizeTextField = BasicComponentFactory.createTextField(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_STORAGE_HISTORY_SIZE));
		maximumAllowedRateTextField = BasicComponentFactory.createIntegerField(vSensorConfigPresentationModel.getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE));
		marUnlimitedCheckBox = BasicComponentFactory.createCheckBox(vSensorConfigPresentationModel.getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED), "Unlimited");
	}

}
