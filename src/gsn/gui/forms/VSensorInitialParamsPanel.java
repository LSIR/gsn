package gsn.gui.forms;

import gsn.gui.beans.VSensorConfigModel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class VSensorInitialParamsPanel {

	private PresentationModel presentationModel;
	
	private JTextField classNameTextField;
	
	private KeyValueEditorPanel keyValueEditorPanel;

	public VSensorInitialParamsPanel(PresentationModel presentationModel) {
		this.presentationModel = presentationModel;
	}

	public JComponent createPanel() {
		initComponents();
		
		FormLayout layout = new FormLayout("right:pref, 4dlu, max(pref;150dlu), pref:g", "pref, 8dlu, pref, 5dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Class name", cc.xy(1, 1));
		builder.add(classNameTextField, cc.xy(3, 1));
		builder.addSeparator("Initial parameters", cc.xyw(1, 3, 4));
		builder.add(keyValueEditorPanel.createPanel(), cc.xyw(1, 5, 4));
		return builder.getPanel();
	}

	private void initComponents() {
		classNameTextField = BasicComponentFactory.createTextField(presentationModel.getModel(VSensorConfigModel.PROPERTY_MAIN_CLASS));
		keyValueEditorPanel = new KeyValueEditorPanel(((VSensorConfigModel)presentationModel.getBean()).getMainClassInitialParams());
	}

}
