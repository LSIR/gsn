package gsn.gui;

import gsn.beans.StreamElement;
import gsn.utils.protocols.AbstractHCIQuery;
import gsn.utils.protocols.ProtocolManager;
import gsn.vsensor.HCIProtocolGUIVS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Jérôme Rousselot <jerome.rousselot@csem.ch>
 *
 */

public class JHCIProtocolControl extends JFrame {

	private static final transient Logger logger = Logger.getLogger( JHCIProtocolControl.class );
	private static final String Default_jqueries_tooltip_text = "Choose a query to send to the mote.";
	private JPanel controlPanel;
	private JComboBox jqueries = new JComboBox();
	private JLabel labelQueries = new JLabel("Choose a query: ");
	private JLabel labelParameters = new JLabel("Query parameters:");
	private JLabel labelQueryDescription = new JLabel("Select a query to see a detailed description of it.");
	private JLabel statusBarLabel = new JLabel("This is the status bar.");
	private JButton buttonSendQuery = new JButton("Send query");
	private JPanel parametersPanel;
	private JTextArea[] parametersValues;
	private JLabel[] parametersLabels;
	private JPanel statusBar;
	private SwingLogView displayArea = new SwingLogView();
	private ProtocolManager manager;

	public JHCIProtocolControl(ProtocolManager manager) { 
		super();
		this.manager = manager;
		setTitle("GSN Host Controller Interface for protocol " + manager.getProtocolName());
		initComponents();
		initEvents();
		pack();
		setVisible(true);
		setPreferredSize(new Dimension(800, 600));
		if(logger.isDebugEnabled())
			logger.debug("GUI started.");

	}
	/**
	 * @param inputStreamName
	 * @param streamElement
	 */
	public void displayData(String inputStreamName, StreamElement streamElement) {

		displayArea.doLogRx(streamElement.getData()[0].toString());
	}
	/**
	 * 
	 */
	private void initEvents() {
		buttonSendQuery.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractHCIQuery query = manager.getQuery((String) jqueries.getSelectedItem());
				if(query != null) {
					if(manager.getCurrentState() == ProtocolManager.ProtocolStates.READY) {

						Vector<Object> queryParams = null;
						String paramsText="";
						if(parametersValues != null) {
							queryParams = new Vector<Object>();
							for(JTextArea param: parametersValues) {
								queryParams.add(param.getText());
								paramsText=paramsText + ", " + param.getText();
							}
							paramsText = paramsText.substring(1);
						}
						byte[] bytes = manager.sendQuery(query.getName(), queryParams);
						displayArea.doLogTx("\nSent query type="+query.getName() + ", params="+ paramsText + "\n");
						displayArea.doLogTx(bytes);

						//	JOptionPane.showMessageDialog(gui, "Sorry, No wrapper available yet.", "Cannot send query", JOptionPane.ERROR);
					}
				} else {
					JOptionPane.showMessageDialog(null, "You should first choose a query.", "No query selected", JOptionPane.ERROR);
				}
			}

		});

		jqueries.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractHCIQuery query = manager.getQuery((String)jqueries.getSelectedItem());
				if(query != null) {
					jqueries.setToolTipText(query.getQueryDescription());
					labelQueryDescription.setText(query.getQueryDescription());
					getContentPane().remove(controlPanel);
					controlPanel.removeAll();
					parametersPanel.removeAll();
					buildParametersPanel(query);
					buildControlPanel();
					getContentPane().add(controlPanel);
					invalidate();
					pack();
					repaint();
				} else {
					jqueries.setToolTipText(Default_jqueries_tooltip_text);
					labelQueryDescription.setText(null);
				}
			}
		});

		//jqueries.setSelectedIndex(0);
	}
	/**
	 * 
	 */
	private void initComponents() {
		initJQueries();
		buildParametersPanel(null);
		initStatusBar();
		buildControlPanel();
		getContentPane().add(controlPanel);

	}

	private void buildControlPanel() {

		FormLayout layout = new FormLayout(
				"center:pref, center:pref:grow, center:pref", // 3 cols
		"top:pref, pref, fill:pref:grow, pref"); // 4 rows
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		CellConstraints cc = new CellConstraints();

		builder.add(labelQueries, cc.xy(1,1));
		builder.add(jqueries, cc.xy(1,2));
		//builder.add(labelQueryDescription, cc.xy(1,1));
		builder.add(labelParameters, cc.xy(2,1));
		builder.add(parametersPanel, cc.xy(2,2));
		builder.add(buttonSendQuery, cc.xy(3, 2));
		builder.add(displayArea, cc.xywh(1,3,3,1));
		builder.add(statusBar, cc.xywh(1,4,3,1));
		controlPanel=builder.getPanel();	
	}
	private void initStatusBar() {
		statusBar = new JPanel();
		statusBar.add(statusBarLabel);
	}

	private void initJQueries() {
		for(AbstractHCIQuery query: manager.getQueries())
			jqueries.addItem(query.getName());
		jqueries.setToolTipText(Default_jqueries_tooltip_text);
	}

	private void buildParametersPanel(AbstractHCIQuery query) {
		if(query != null && query.getParamsDescriptions() != null) {
			int nbParameters = query.getParamsDescriptions().length;
			String rowsLayout = "pref";
			for(int i = 1; i < nbParameters; i++)
				rowsLayout = rowsLayout + ", pref"; 
			FormLayout layout = new FormLayout("right:pref,pref", rowsLayout); 
			DefaultFormBuilder builder = new DefaultFormBuilder(layout);
			CellConstraints cc = new CellConstraints();

			parametersLabels = new JLabel[nbParameters];
			parametersValues = new JTextArea[nbParameters];
			for(int i = 0; i < nbParameters; i++) {
				parametersLabels[i] = new JLabel(query.getName());
				parametersValues[i] = new JTextArea("enter value " + i + " here.");
				if(query.getParamsDescriptions()[i] != null && ! query.getParamsDescriptions()[i].trim().equals("")) {
					parametersLabels[i].setToolTipText(query.getParamsDescriptions()[i]);
					parametersValues[i].setToolTipText(query.getParamsDescriptions()[i]);
				}
				
				builder.add(parametersLabels[i], cc.xy(1, i+1));
				builder.add(parametersValues[i], cc.xy(2, i+1));
			}
			if(parametersPanel != null)
				parametersPanel.removeAll();
			parametersPanel = builder.getPanel();
		} else {
			parametersLabels = null;
			parametersValues = null;
			parametersPanel = new JPanel();
		}

	}



	public class SwingLogView extends JPanel {

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
		private static final int DEFAULT_LOG_SIZE = 100000, MAX_LOG_SIZE = 1000000, MIN_LOG_SIZE = 2000;
		private static final int SPINNER_STEP = 1000;

		public SwingLogView() {
			super();
			// create text area to hold the log messages
			setLayout(new BorderLayout());
			add(BorderLayout.CENTER, getScrollPane());
			add(BorderLayout.SOUTH, getControlBar());
		}

		private JPanel getControlBar() {

			controlPane = new JPanel(new GridBagLayout());
			logSize = new JSpinner(new SpinnerNumberModel(DEFAULT_LOG_SIZE,
					MIN_LOG_SIZE, MAX_LOG_SIZE, SPINNER_STEP));
			logSize
			.setToolTipText("You can change here how much history is displayed in this window.");

			allowScroll = new JCheckBox(
			"Scroll log window when new data is available.");
			allowScroll.setSelected(true);
			allowScroll
			.setToolTipText("Disable this to inspect old data.");
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

		public void doLogTx(String log) {
			doLog(log, false);
		}

		public void doLogTx(byte[] log) {
			if (log != null)
				doLog(log, false);
		}
		public void doLogRx(String log) {
			doLog(log, true);
		}
		public void doLogRx(byte[] log) {
			doLog(log, true);
		}

		public void doLog(byte[] log, boolean received) {
			StringBuffer buffer = new StringBuffer();
			int j = 0;
			byte c;
			boolean hexmode = false;
			logger.debug("Starting decoding of a byte array");
			while ( j < log.length) {
				c = log[j];
				if(c < 48 || c > 57 && c < 65 || c > 90 && c < 97 || c > 122) {
					// write current buffer
					try {
						doc.insertString(doc.getEndPosition().getOffset(), buffer.toString(), doc.getStyle(STANDARD_TEXT));
						buffer = new StringBuffer();
					} catch(BadLocationException e) {
						logger.error(e);
					}

					// switch to hexadecimal display
					buffer.append(" 0x");
					hexmode = true;
					int value = c;
					if(j+1 < log.length) {
						j=j+1;
						value = value*16 + log[j];
					}
					logger.debug("decoded value: " + value + " and prepared it for output as: 0x" + Integer.toHexString(value));
					buffer.append(Integer.toHexString(value));
				} else {
					byte[] tempbuffer = new byte[1];
					tempbuffer[0] = c;
					buffer.append(tempbuffer);
				}

				if(hexmode) {
					try {
						doc.insertString(doc.getEndPosition().getOffset(), buffer.toString(), doc.getStyle(GREEN_TEXT));
					} catch(BadLocationException e) {
						logger.error(e);
					}

					hexmode = false;
				}

			}

			try {
				doc.insertString(doc.getEndPosition().getOffset(), buffer.toString(), doc.getStyle(STANDARD_TEXT));
				buffer = new StringBuffer();
			} catch(BadLocationException e) {
				logger.error(e);
			}

		}
		/**
		 * adds data to the display, and nicely formats hex values.
		 * 
		 */
		public void doLog(String log, boolean received) {
			String[] pieces = log.split("\\s");
			StringBuilder sbuilder;
			try {
				int caretPosition = logMessagesDisp.getCaretPosition();
				if(received) {
					doc.insertString(doc.getEndPosition().getOffset(), "\nRX:", doc.getStyle(BOLD_TEXT));
				} else {
					doc.insertString(doc.getEndPosition().getOffset(), "\nTX:", doc.getStyle(BOLD_TEXT));
				}


				doc.insertString(doc.getEndPosition().getOffset(), log, doc.getStyle(STANDARD_TEXT));

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

		public void clear() {
			try {
				doc.remove(0, doc.getLength());
			} catch (BadLocationException e) {
				System.err.print(e.getStackTrace());
			}
		}
	}
}
