/**
 * 
 * @author Jerome Rousselot 
  */


package gsn.vsensor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.protocols.AbstractHCIProtocol;
import gsn.utils.protocols.AbstractHCIQuery;
import gsn.utils.protocols.ProtocolManager;
import gsn.utils.protocols.EPuck.SerComProtocol;
import gsn.wrappers.StreamProducer;
/**
 * @author jerome
 *
 */
public class HCIProtocolGUIVS extends AbstractVirtualSensor {

	private static final transient Logger logger = Logger.getLogger( HCIProtocolGUIVS.class );

	private TreeMap < String , String >   params;

	private ProtocolManager protocolManager;
	private AbstractHCIProtocol protocol; 
	private StreamProducer outputWrapper = null;
	private JHCIProtocolControl gui;

   private VSensorConfig virtualSensorConfiguration;

	public boolean initialize ( HashMap map ) {	
		boolean toReturn = super.initialize( map );
		if ( toReturn == false ) return false;
      virtualSensorConfiguration= ((VSensorConfig) map.get( VirtualSensorPool.VSENSORCONFIG ));
      params = virtualSensorConfiguration.getMainClassInitialParams( );
		ClassLoader loader = getClass().getClassLoader();
		try {
			Class protocolClass = loader.loadClass(params.get("HCIProtocolClass"));
			protocol = (AbstractHCIProtocol) protocolClass.newInstance();
		} catch (InstantiationException e) {
			logger.error(e);
			toReturn = false;
		} catch (IllegalAccessException e) {
			logger.error(e);
			toReturn = false;
		} catch (ClassNotFoundException e) {
			logger.error(e);
			toReturn = false;
		}
		protocolManager = new ProtocolManager(protocol);
		if(logger.isDebugEnabled() && protocol != null && protocolManager != null)
			logger.debug("Successfully loaded protocol class " + params.get("HCIProtocolClass"));

		// Then, create GUI
		gui = new JHCIProtocolControl();

		return toReturn;
	}
	/* (non-Javadoc)
	 * @see gsn.vsensor.VirtualSensor#dataAvailable(java.lang.String, gsn.beans.StreamElement)
	 */
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		if(outputWrapper == null) {
			// first try to find a wrapper on which we can write to.
			outputWrapper = virtualSensorConfiguration.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
		}
		gui.displayData(inputStreamName, streamElement);
	}

	private class JHCIProtocolControl extends JFrame {
		private static final String Default_jqueries_tooltip_text = "Choose a query to send to the mote.";
		JPanel controlPanel;
		JComboBox jqueries = new JComboBox();
		JLabel labelQueries = new JLabel("Choose a query: ");
		JLabel labelParameters = new JLabel("Query parameters:");
		JLabel labelQueryDescription = new JLabel("Select a query to see a detailed description of it.");
		JLabel statusBarLabel = new JLabel("This is the status bar.");
		JButton buttonSendQuery = new JButton("Send query");
		JTextArea displayArea = new JTextArea("This is the display area");

		JPanel parametersPanel;
		JTextArea[] parametersValues;
		JLabel[] parametersLabels;
		private JPanel statusBar;

		public JHCIProtocolControl() { 
			super();
			setTitle("GSN Host Controller Interface for protocol " + protocol.getName());
			initComponents();
			initEvents();
			pack();
			setVisible(true);
			if(logger.isDebugEnabled())
				logger.debug("GUI started.");
		}
		/**
		 * @param inputStreamName
		 * @param streamElement
		 */
		public void displayData(String inputStreamName, StreamElement streamElement) {
			String textData = "";
			for(int i = 0; i < streamElement.getFieldNames().length; i++)
				textData = textData + ", " + streamElement.getFieldNames()[i] + "=" + streamElement.getData()[i];
			textData = textData.substring(1);
			displayArea.append("\nReceived data (stream="+inputStreamName+"): "+ textData + "\n");
			
		}
		/**
		 * 
		 */
		private void initEvents() {
			buttonSendQuery.addActionListener( new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							AbstractHCIQuery query = protocol.getQuery((String) jqueries.getSelectedItem());
							if(query != null) {
								if(protocolManager.getCurrentState() == ProtocolManager.ProtocolStates.READY) {
									Vector<Object> queryParams = new Vector<Object>();
									String paramsText="";
									for(JTextArea param: parametersValues) {
										queryParams.add(param.getText());
										paramsText=paramsText + ", " + param.getText();
									}
									paramsText = paramsText.substring(1);	
									if(outputWrapper != null) {
										boolean b = protocolManager.sendQuery(query.getName(), queryParams, outputWrapper);
										displayArea.append("\nSent query type="+query.getName() + ", params="+ paramsText + "\n");
								} else
									JOptionPane.showMessageDialog(gui, "Sorry, No wrapper available yet.", "Cannot send query", JOptionPane.ERROR);
								}
							} else {
								JOptionPane.showMessageDialog(gui, "You should first choose a query.", "No query selected", JOptionPane.ERROR);
							}
						}
						
					});
			
			jqueries.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AbstractHCIQuery query = protocol.getQuery((String)jqueries.getSelectedItem());
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
			
			jqueries.setSelectedIndex(0);
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
			builder.add(buttonSendQuery, cc.xy(3, 1));
			builder.add(displayArea, cc.xywh(1,3,3,1));
			builder.add(statusBar, cc.xywh(1,4,3,1));
			controlPanel=builder.getPanel();	
		}
		private void initStatusBar() {
			statusBar = new JPanel();
			statusBar.add(statusBarLabel);
		}

		private void initJQueries() {
			for(AbstractHCIQuery query: protocol.getQueries())
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
					if(query.getParamsDescriptions()[i] != null && ! query.getParamsDescriptions()[i].trim().equals(""))
						parametersLabels[i] = new JLabel(query.getParamsDescriptions()[i]);
					else
						parametersLabels[i] = new JLabel("Parameter " + i + ": ");
					parametersValues[i] = new JTextArea("enter value " + i + " here.");
					parametersValues[i].setToolTipText(query.getParamsDescriptions()[i]);
					builder.add(parametersLabels[i], cc.xy(1, i+1));
					builder.add(parametersValues[i], cc.xy(2, i+1));
				}
				if(parametersPanel != null)
					parametersPanel.removeAll();
				parametersPanel = builder.getPanel();
			} else {
				parametersPanel = new JPanel();
			}

		}
	}
}
