/**
 * 
 */
package gsn.gui.forms;

import gsn.utils.ValidityTools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SwingLogView extends JPanel {
	
	public static final transient Logger logger    = Logger.getLogger( SwingLogView.class );
	   
	// UI attributes
	private JTextPane logMessagesDisp; // display area

	private JScrollPane scrollPane;

	private Style style_red, style_green, style_blue;

	private Style style_bold, style_classic;

	private StyledDocument doc;

	private JPanel controlPane;

	private JSpinner logSize;

	private JCheckBox allowScroll;

	/* Constants */
	public static final int STARTED = 0;

	public static final int STOPPED = 2;

	public static final String RED_TEXT = "RED_TEXT";

	public static final String BLUE_TEXT = "BLUE_TEXT";

	public static final String GREEN_TEXT = "GREEN_TEXT";

	public static final String BOLD_TEXT = "BOLD_TEXT";

	public static final String STANDARD_TEXT = "STANDARD_TEXT";

	public static final String STYLE_DEBUG = STANDARD_TEXT;

	public static final String STYLE_INFO = GREEN_TEXT;

	public static final String STYLE_WARN = BLUE_TEXT;

	public static final String STYLE_ERROR = RED_TEXT;

	public static final String STYLE_TIMESTAMP = BOLD_TEXT;

	private static final String LOG4J_INFO = "INFO";

	private static final String LOG4J_WARN = "WARN";

	private static final String LOG4J_ERROR = "ERROR";

	private static final int DEFAULT_LOG_SIZE = 100000, MAX_LOG_SIZE = 1000000,
	MIN_LOG_SIZE = 2000;

	private Timer updateTimer;

	private static final int LOGVIEWER_REFRESH_INTERVAL = 100; // refresh
	// interval in
	// ms

	private static final int LOGVIEWER_INITIAL_DELAY = 10;

	private static final int SPINNER_STEP = 1000;

	private String log_file;

	private BufferedReader reader;

	private Date startLoggingDate;

	// private static final String log4j_timestamp_regex =
	// "\\[\\d\\d\\d\\d-\\d\\d-\\d\\d";

	public SwingLogView(String log_file) {
		super();
		// create text area to hold the log messages
		add(BorderLayout.CENTER, getScrollPane());
		
		add(BorderLayout.PAGE_END, getControlBar());
		this.log_file = log_file;
	}

	private JPanel getControlBar() {

		controlPane = new JPanel(new GridBagLayout());
		logSize = new JSpinner(new SpinnerNumberModel(DEFAULT_LOG_SIZE,
				MIN_LOG_SIZE, MAX_LOG_SIZE, SPINNER_STEP));
		logSize
		.setToolTipText("You can change here how much log history is displayed in this window.");

		allowScroll = new JCheckBox(
		"Scroll log window when new data is available.");
		allowScroll.setSelected(true);
		allowScroll
		.setToolTipText("Disable this to inspect old logs statements.");
		/*
		 * GridBagConstraints c = new GridBagConstraints(); c.gridx=0;
		 * c.gridy=0; c.insets = new Insets(0,5,5,0);
		 * c.anchor=GridBagConstraints.LINE_START; controlPanel.add(logSize, c);
		 * c.gridx=1; controlPanel.add(new JLabel("History size."), c);
		 * c.gridx=3; c.anchor=GridBagConstraints.LINE_END;
		 * controlPanel.add(allowScroll, c);
		 */
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"r:pref,4dlu,pref:g,8dlu,r:pref,4dlu,pref:g", ""));
		builder.append(logSize);
		
		builder.append(new JLabel("History size."));
		builder.append(allowScroll);
		controlPane = builder.getPanel();
		return controlPane;
	}

	public void doLog(Vector<String> logs) {
		for (String line : logs) {
			if (!line.trim().equals(""))
				doLog(line);
		}
	}

	/**
	 * Analyze what has been read from the log file, and format it nicely
	 * for graphical output.
	 * 
	 * @param log
	 *                The log message to be displayed in the text area.
	 *                Should start with a log level, a white space, a
	 *                timestamp and the rest of the log message.
	 */

	public void doLog(String log) {
		String[] pieces = log.split("\\s");
		StringBuilder sbuilder;
		try {
			int caretPosition = logMessagesDisp.getCaretPosition();
			int msgPosition = 0;
			boolean thisIsaAMessageStart = false;
			// find out if this is the start of a message and format it
			// accordingly.
			if (pieces[0].trim().equals(LOG4J_INFO)) {
				doc.insertString(doc.getEndPosition().getOffset(), pieces[msgPosition++], doc
						.getStyle(STYLE_INFO));
				thisIsaAMessageStart = true;
			} else if (pieces[0].trim().equals(LOG4J_WARN)) {
				doc.insertString(doc.getEndPosition().getOffset(), pieces[msgPosition++], doc
						.getStyle(STYLE_WARN));
				thisIsaAMessageStart = true;
			} else if (pieces[0].trim().equals(LOG4J_ERROR)) {
				doc.insertString(doc.getEndPosition().getOffset(), pieces[msgPosition++], doc
						.getStyle(STYLE_ERROR));
				thisIsaAMessageStart = true;
			}
			if (thisIsaAMessageStart) {
				// extract the timestamp and make a nice formatting
				sbuilder = new StringBuilder();
				sbuilder.append(" ");
				sbuilder.append(pieces[2]);
				sbuilder.append(" ");
				sbuilder.append(pieces[3]);
				doc.insertString(doc.getEndPosition().getOffset(), sbuilder.toString(), doc
						.getStyle(STYLE_TIMESTAMP));
				msgPosition = 4;
			}

			// build a String with the remainder of the text and append it
			// to the textpane
			sbuilder = new StringBuilder();
			for (int i = msgPosition; i < pieces.length; i++) {
				sbuilder.append(" ");
				sbuilder.append(pieces[i]);
			}
			doc.insertString(doc.getEndPosition().getOffset(), sbuilder.toString(), doc
					.getStyle(STANDARD_TEXT));
			// overflow control
			if (doc.getLength() > (Integer) logSize.getValue()) {
				// doc.remove(0, doc.getLength()-MAX_LOG_SIZE);
				int lengthToRemove=0;
				lengthToRemove = doc.getText(0, doc.getEndPosition().getOffset() - (Integer) logSize.getValue()). lastIndexOf('\n');
				if(lengthToRemove==0)
					lengthToRemove = doc.getText(0, doc.getEndPosition().getOffset()).indexOf('\n');
				doc.remove(0, doc.getText(0, doc.getEndPosition().getOffset() - (Integer) logSize.getValue()). lastIndexOf('\n'));
			}
			// doc.insertString(doc.getLength(), log,
			// doc.getStyle(GREEN_TEXT));

			// add a carriage return at the end of the line.
			doc.insertString(doc.getEndPosition().getOffset(), "\n", doc.getStyle(STANDARD_TEXT));
			if (allowScroll.isSelected()) {
				logMessagesDisp.setCaretPosition(doc.getEndPosition().getOffset()-1); // Set the scrollbar at the bottom
			} else {
				logMessagesDisp.setCaretPosition(caretPosition);
			}
		} catch (BadLocationException e) {
			logger.warn(e.getMessage(),e);
			System.err.println ("---------------------------------------------");
			System.err.println("GSN-Installer encountered an error. Please help us make this software better !");
			System.err.println("Simply send the files in the logs directory to jerome.rousselot@gmail.com.");
			System.err.println("The GSN Development Team thanks you for your help !");
			System.err.println ("---------------------------------------------");
		}
	}

	/**
	 * Creates a scrollable text area
	 */
	private JScrollPane getScrollPane() {
		logMessagesDisp = new JTextPane();
		doc = logMessagesDisp.getStyledDocument();
		// Define text styles
		style_red = doc.addStyle(RED_TEXT, null);
		StyleConstants.setForeground(style_red, Color.red);

		style_green = doc.addStyle(GREEN_TEXT, null);
		StyleConstants.setForeground(style_green, Color.green);

		style_blue = doc.addStyle(BLUE_TEXT, null);
		StyleConstants.setForeground(style_blue, Color.blue);

		style_bold = doc.addStyle(BOLD_TEXT, null);
		StyleConstants.setBold(style_bold, true);

		style_classic = doc.addStyle(STANDARD_TEXT, null);
		StyleConstants.setForeground(style_classic, Color.black);

		scrollPane = new JScrollPane(logMessagesDisp);
		scrollPane
		.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane
		.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		return scrollPane;
	}

	public void stopWatchingLog() {
		try {
			// in case of immediate error, make sure we display the log
			// output
			Thread.sleep(Math.max(LOGVIEWER_INITIAL_DELAY,
					LOGVIEWER_REFRESH_INTERVAL));

			updateTimer.stop();
			if (reader != null) {
				reader.close();
				reader = null;
			}
		} catch (IOException ioe) {
			System.out.println("Couldn't close log file " + log_file
					+ " correctly: " + ioe);
		} catch (InterruptedException e) {

		}
	}

	public void clear() {
		try {
			doc.remove(0, doc.getLength());
		} catch (BadLocationException e) {
			System.err.print(e.getStackTrace());
		}
	}

}