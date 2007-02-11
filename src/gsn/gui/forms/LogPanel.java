/**
 * 
 * @author Jerome Rousselot
 */
package gsn.gui.forms;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * @author jerome
 * 
 */
public class LogPanel extends JPanel {

	private SwingLogView gsnAppender;
	public static final String GSN_LOG_FILE = "logs/gsn.log";

	public LogPanel() {
		super();
		gsnAppender = new SwingLogView(GSN_LOG_FILE);
		add(gsnAppender);
		setToolTipText("Logs stored at : logs/gsn.log");
	}

	public SwingLogView getGSNLogView() {
		return gsnAppender;
	}

}