/**
 * 
 * @author Jerome Rousselot
 */
package gsn.gui.forms;

import javax.swing.JTabbedPane;

/**
 * @author jerome
 * 
 */
public class LogPanel extends JTabbedPane {

    private SwingLogView gsnAppender, dirAppender;

    public static final String DIR_LOG_FILE = "logs/gsn-dir.log";

    public static final String GSN_LOG_FILE = "logs/gsn.log";

    public LogPanel() {
	super();
	gsnAppender = new SwingLogView(GSN_LOG_FILE);
	dirAppender = new SwingLogView(DIR_LOG_FILE);
	add(gsnAppender);
	setTitleAt(0, "GSN Log");
	setToolTipTextAt(0, "Logs stored at : logs/gsn.log");
	add(dirAppender);
	setTitleAt(1, "Directory server log");
	setToolTipTextAt(1, "Logs stored at : logs/gsn-dir.log");
    }

    public SwingLogView getGSNLogView() {
	return gsnAppender;
    }

    public SwingLogView getDirLogView() {
	return dirAppender;
    }
}