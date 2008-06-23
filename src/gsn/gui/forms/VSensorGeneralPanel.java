package gsn.gui.forms;

import gsn.gui.beans.VSensorConfigModel;
import gsn.gui.beans.VSensorConfigPresentationModel;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.view.ValidationComponentUtils;

public class VSensorGeneralPanel {

	private VSensorConfigPresentationModel presentationModel;
	private JTextField nameTextField;
	private JTextField priorityTextField;
	private JTextArea descriptionTextArea;
	private JTextField poolSizeTextField;
	private JTextField storageSizeTextField;
	private JTextField maximumAllowedRateTextField;
	private JCheckBox marUnlimitedCheckBox;
	private JComponent editorPanel;

	public VSensorGeneralPanel(VSensorConfigPresentationModel presentationModel) {
		this.presentationModel = presentationModel;
	}

	public Component createPanel() {
		initComponents();
		initComponentAnnotations();
		initEventHandling();
		
		KeyValueEditorPanel keyValueEditorPanel = new KeyValueEditorPanel(((VSensorConfigModel)presentationModel.getBean()).getAddressing());
		
		FormLayout layout = new FormLayout("pref:g", "pref, pref:g, pref, pref:g");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addSeparator("General properties", cc.xy(1, 1));
		editorPanel = buildGeneralFieldsPanel();
		builder.add(editorPanel, cc.xy(1, 2));
		builder.addSeparator("Addressing", cc.xy(1, 3));
		builder.add(keyValueEditorPanel.createPanel(), cc.xy(1, 4));
		
		updateComponentTreeMandatoryAndSeverity(presentationModel.getValidationResultModel().getResult());
		return builder.getPanel();
	}
	
	private JComponent buildGeneralFieldsPanel() {
		FormLayout layout = new FormLayout(
				"right:pref, 3dlu, min(pref;75dlu), 7dlu, right:pref, 3dlu, min(pref;50dlu), 3dlu, pref, 3dlu",
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, min(pref;100dlu):g");
		layout.setColumnGroups(new int[][]{{1, 5}, {3, 7}});
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Name", cc.xy(1, 1));
		builder.add(nameTextField, cc.xy(3, 1));
		builder.addLabel("Priority", cc.xy(5, 1));
		builder.add(priorityTextField, cc.xyw(7, 1, 3));
		builder.addLabel("Password" , cc.xy(1, 3));
		builder.addLabel("Pool size", cc.xy(5, 3));
		builder.add(poolSizeTextField, cc.xyw(7, 3, 3));
		builder.addLabel("Storage size" , cc.xy(1, 5));
		builder.add(storageSizeTextField, cc.xy(3, 5));
		builder.addLabel("Maximum rate", cc.xy(5, 5));
		builder.add(maximumAllowedRateTextField, cc.xy(7, 5));
		builder.add(marUnlimitedCheckBox, cc.xy(9, 5));
		builder.addLabel("Description", cc.xy(1, 7));
		builder.add(new JScrollPane(descriptionTextArea), cc.xyw(3, 7, 7));
		return builder.getPanel();
	}

	private void initComponents() {
		nameTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(VSensorConfigModel.PROPERTY_NAME));
		priorityTextField = BasicComponentFactory.createIntegerField(presentationModel.getModel(VSensorConfigModel.PROPERTY_PRIORITY));
		descriptionTextArea = BasicComponentFactory.createTextArea(presentationModel.getModel(VSensorConfigModel.PROPERTY_DESCRIPTION));
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setWrapStyleWord(true);
		
		poolSizeTextField = BasicComponentFactory.createIntegerField(presentationModel.getModel(VSensorConfigModel.PROPERTY_LIFECYCLE_POOL_SIZE));
		storageSizeTextField = BasicComponentFactory.createTextField(presentationModel.getModel(VSensorConfigModel.PROPERTY_STORAGE_HISTORY_SIZE));
		maximumAllowedRateTextField = BasicComponentFactory.createIntegerField(presentationModel.getComponentModel(VSensorConfigModel.PROPERTY_OUTPUT_STREAM_RATE));
		marUnlimitedCheckBox = BasicComponentFactory.createCheckBox(presentationModel.getModel(VSensorConfigModel.PROPERTY_RATE_UNLIMITED), "Unlimited");
	}
	
	private void initComponentAnnotations() {
		ValidationComponentUtils.setMandatory(nameTextField, true);
		ValidationComponentUtils.setMessageKey(nameTextField, "VSensorConfig.Name");
		ValidationComponentUtils.setMessageKey(storageSizeTextField, "VSensorConfig.History Size");
	}
	
	private void initEventHandling() {
		presentationModel.getValidationResultModel().addPropertyChangeListener(ValidationResultModel.PROPERTYNAME_RESULT,
				new ValidationChangeHandler());
	}
	
	private void updateComponentTreeMandatoryAndSeverity(ValidationResult result) {
		ValidationComponentUtils.updateComponentTreeSeverity(editorPanel, result);
		ValidationComponentUtils.updateComponentTreeMandatoryAndBlankBackground(editorPanel);
	}
	
	private final class ValidationChangeHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			updateComponentTreeMandatoryAndSeverity((ValidationResult) evt.getNewValue());
		}
	}

}
