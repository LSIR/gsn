package gsn.gui.forms;

import gsn.beans.VSensorConfig;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class VSensorFormPanel {
	private JPanel panel;

	private VSensorConfig vSensorConfig;
	
	public VSensorFormPanel(){
		initGUI();
	}

	private void initGUI() {
		JTabbedPane mainTabbedPane = new JTabbedPane();
		JTabbedPane detailsTabbedPane = createDetailsTabbedPane();
		JTabbedPane inputStreamsTabbedPane = createInputStreamTabbedPane();
	}


	private JTabbedPane createDetailsTabbedPane() {
		JTabbedPane detailsTabbedPane = new JTabbedPane();
		
		return detailsTabbedPane;
	}
	
	private JTabbedPane createInputStreamTabbedPane() {
		JTabbedPane inputStreamsTabbedPane = new JTabbedPane();
		return inputStreamsTabbedPane;
	}
}
