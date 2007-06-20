package gsn.gui.forms;

import javax.swing.JComponent;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gsn.gui.beans.AddressBeanModel;

public class AddressBeanEditorPanel {
	
	private AddressBeanModel addressBeanModel;
	private PresentationModel presentationModel;
	private KeyValueEditorPanel keyValueEditorPanel;

	public AddressBeanEditorPanel(AddressBeanModel addressBeanModel){
		this.addressBeanModel = addressBeanModel;
		presentationModel = new PresentationModel(addressBeanModel);
	}
	
	public void setModel(AddressBeanModel addressBeanModel){
		this.addressBeanModel = addressBeanModel;
		presentationModel.setBean(addressBeanModel);
		if(addressBeanModel != null)
			keyValueEditorPanel.setListModel(addressBeanModel.getPredicates());
		else
			keyValueEditorPanel.setListModel(null);
	}
	
	public JComponent createPanel(){
		initComponents();
		
		FormLayout layout = new FormLayout("right:max(pref;40), 4dlu, max(pref;150dlu):g", "pref:g");
		PanelBuilder builder = new PanelBuilder(layout);
//		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Predicates", cc.xy(1, 1, "right, top"));
		builder.add(keyValueEditorPanel.createPanel(), cc.xy(3, 1));
		return builder.getPanel();
	}

	private void initComponents() {
		keyValueEditorPanel = new KeyValueEditorPanel(addressBeanModel == null ? null : addressBeanModel.getPredicates());
	}
}
