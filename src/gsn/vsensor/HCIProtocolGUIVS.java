package gsn.vsensor;



import gsn.utils.ValidityTools;
import gsn.gui.JHCIProtocolControl;
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
import java.awt.Component;
import java.awt.Dimension;
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
import gsn.wrappers.Wrapper;
/**
 * @author Jerome Rousselot <jerome.rousselot@csem.ch>
 *
 */
public class HCIProtocolGUIVS extends AbstractVirtualSensor {

	private static final transient Logger logger = Logger.getLogger( HCIProtocolGUIVS.class );

	private TreeMap < String , String >   params;

	private ProtocolManager protocolManager;
	private AbstractHCIProtocol protocol; 
	private Wrapper outputWrapper = null;
	private JHCIProtocolControl gui;
	private VSensorConfig vsensor;

	public boolean initialize ( HashMap map ) {	
		boolean toReturn = super.initialize( map );
		if ( toReturn == false ) return false;
		vsensor = ((VSensorConfig) map.get( VirtualSensorPool.VSENSORCONFIG ));
		params = vsensor.getMainClassInitialParams( );		
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
		outputWrapper = vsensor.getInputStream( "input1" ).getSource( "source1" ).getActiveSourceProducer( );
		protocolManager = new ProtocolManager(protocol, outputWrapper);
		if(logger.isDebugEnabled() && protocol != null && protocolManager != null)
			logger.debug("Successfully loaded protocol class " + params.get("HCIProtocolClass"));
		
		
		// Then, create GUI
		gui = new JHCIProtocolControl(protocolManager);

		return toReturn;
	}
	/* (non-Javadoc)
	 * @see gsn.vsensor.VirtualSensor#dataAvailable(java.lang.String, gsn.beans.StreamElement)
	 */
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {

		gui.displayData(inputStreamName, streamElement);
	}


}
