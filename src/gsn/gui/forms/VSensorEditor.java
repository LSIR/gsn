package gsn.gui.forms;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import gsn.beans.VSensorConfig;
import gsn.gui.beans.VSensorConfigModel;
import gsn.gui.beans.VSensorConfigPresentationModel;
import gsn.gui.util.GUIUtil;

import com.jgoodies.binding.PresentationModel;

public class VSensorEditor {
	private PresentationModel presentationModel;
	private JDialog dialog;
	
	public VSensorEditor(VSensorConfig vSensorConfig){
		presentationModel = new VSensorConfigPresentationModel(new VSensorConfigModel(vSensorConfig));
		dialog = new JDialog();
		dialog.setTitle("Virtual Sensor Editor");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setContentPane(buildPanel());
		dialog.pack();
		GUIUtil.locateOnOpticalScreenCenter(dialog);
	}
	
	public void showDialog(){
		dialog.setVisible(true);
	}

	private JComponent buildPanel() {
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.putClientProperty("jgoodies.noContentBorder", Boolean.TRUE);
		tabbedPane.addTab("General", new VSensorGeneralPanel(presentationModel).createPanel());
		tabbedPane.addTab("Processing Class", new VSensorProcessingClassPanel(presentationModel).createPanel());
		tabbedPane.addTab("Input Streams", new VSensorInputStreamsPanel(presentationModel).createPanel());
		return tabbedPane;
	}
	
	public static void main(String[] args) {
		try {
            UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
        } catch (Exception e) {
            // Likely PlasticXP is not in the class path; ignore.
        }
		VSensorConfig sensorConfig = new VSensorConfig();
		sensorConfig.setName("name");
		new VSensorEditor(sensorConfig).showDialog();
	}
	
}
