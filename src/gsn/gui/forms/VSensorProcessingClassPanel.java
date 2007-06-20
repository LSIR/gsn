package gsn.gui.forms;

import java.awt.Component;
import javax.swing.JTabbedPane;
import com.jgoodies.binding.PresentationModel;

public class VSensorProcessingClassPanel {

	private PresentationModel presentationModel;
	public VSensorProcessingClassPanel(PresentationModel presentationModel) {
		this.presentationModel = presentationModel;
	}

	public Component createPanel() {
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Initial Parameters", new VSensorInitialParamsPanel(presentationModel).createPanel());
		tabbedPane.add("Web Inputs", new VSensorWebInpuPanel(presentationModel).createPanel());
		tabbedPane.add("Output Structure", new VSensorOutputStructurePanel(presentationModel).createPanel());
		return tabbedPane;
	}

}
