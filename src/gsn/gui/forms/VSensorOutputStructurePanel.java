package gsn.gui.forms;

import gsn.gui.beans.VSensorConfigModel;

import java.awt.Component;

import com.jgoodies.binding.PresentationModel;

public class VSensorOutputStructurePanel {

	private PresentationModel presentationModel;

	public VSensorOutputStructurePanel(PresentationModel presentationModel) {
		this.presentationModel = presentationModel;
	}

	public Component createPanel() {
		VSensorConfigModel vSensorConfigModel = (VSensorConfigModel) presentationModel.getBean();
		return new DataFieldEditorPanel(vSensorConfigModel.getOutputStructure()).createPanel();
	}

}
