package gsn.gui;

import gsn.Main;
import gsn.beans.ContainerConfig;
import gsn.gui.forms.GSNConfiguratorPanel;
import gsn.gui.forms.VSensorVisualizerPanel;
import gsn.gui.util.GUIUtils;
import gsn.utils.ValidityTools;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jibx.runtime.JiBXException;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertBluer;

public class GSNConfiguratorFrame extends JFrame {

	// The swing components for this Frame
	private Container contentPane;

	private GSNConfiguratorPanel configuratorPanel;
	
	private VSensorVisualizerPanel vSensorVisualizerPanel;

	private GSNStatusBar statusBar;

	private JMenuBar menuBar;

	private JMenu menuFile, menuEdit, menuOptions, menuHelp;

	private JMenuItem menuNewConfig, menuSave, menuQuit;

	private JMenuItem menuUndo, menuRedo;

	private JMenuItem menuHelpContents, menuAbout;
	private PopupMenu popup;

	// We load all icons here so that we can reuse them
	public static final Icon GSN_ICON = new ImageIcon("icons/gsn.png");

	// Menus titles (for future i18n/l10n)
	private static final String MENU_FILE = "File", MENU_EDIT = "Edit",
	MENU_OPTIONS = "Options", MENU_HELP = "Help";

	private static final String MENU_NEWCONFIG = "New Configuration",
	MENU_SAVE = "Save Configuration", MENU_QUIT = "Quit", MENU_UPDATE="Update";

	private static final String MENU_UNDO = "Undo Action",
	MENU_REDO = "Redo Action", MENU_HELPCONTENTS = "Help Contents...",
	MENU_ABOUT = "About";

	// about dialog text in html
//	private static final String HTML_CODE_ABOUT_DIALOG = "<h1>GSN Control Center</h1><br>"
//	+ "<p>This software has been brought to you by "
//	+ "<a href=\"http://www.xoben.com\">Xoben Technology</a> (http://www.xoben.com).<br>"
//	+ "The Global Sensor Networks (GSN) engine is free software (GPL license) ;<br> the project is hosted at http://globalsn.sourceforge.net .</p>"
//	+ "<br><br><p>&copy; 2006-2007.</p><br>";


	// menus accelerators / mnemonics
	private static final int ACCEL_FILE = KeyEvent.VK_F;

	private static final KeyStroke ACCEL_QUIT = KeyStroke.getKeyStroke(
			new Character('q'), java.awt.event.InputEvent.CTRL_MASK);

	private static final int ACCEL_EDIT = KeyEvent.VK_E;

	private static final int ACCEL_HELP = KeyEvent.VK_H;

	private  Desktop desktop;
	private SystemTray tray;
	private TrayIcon trayIcon = null;
	private ContainerConfig bean;
	/*
	 * Initialize the frame window, close operations, menus, look&feel
	 */
	public GSNConfiguratorFrame(String containerConfigXML, String gsnLog4j,
			String dirLog4j) throws FileNotFoundException, JiBXException {
		super("GSN Middleware GUI");
		bean = ContainerConfig.getConfigurationFromFile(containerConfigXML, gsnLog4j,	dirLog4j);
		configuratorPanel = new GSNConfiguratorPanel(bean);
		initComponents();
		initEvents();
//		initDesktop();
		GUIUtils.locateOnOpticalScreenCenter(this);
		setVisible(true);
	}

	//TODO:requires JDk 6	
//	private void initDesktop() {
//		if(SystemTray.isSupported() && Desktop.isDesktopSupported()) {
//			tray = SystemTray.getSystemTray();
//			desktop = Desktop.getDesktop();
//			Image image = Toolkit.getDefaultToolkit().getImage("icons/gsn.png");
//			ActionListener trayListener = new ActionListener() {
//				public void actionPerformed(ActionEvent arg0) {
//					try {
//						desktop.browse(new URI("http://127.0.0.1:"+Integer.toString(bean.getContainerPort())));
//					} catch(IOException ioe) {
//						System.out.println("GSN encountered an unexpected error: " + ioe.getMessage());
//						ioe.printStackTrace();
//					} catch (URISyntaxException e) {
//						e.printStackTrace();
//					}
//				}
//			};
//			
//	         // create a popup menu
//	         popup = new PopupMenu();
//	         
//	         // create menu item to access web interface
//	         MenuItem openWebItem = new MenuItem("GSN Web Interface");
//	         openWebItem.addActionListener(trayListener);
//	         popup.add(openWebItem);
//	         
//	         // menu item to exit gsn
//	         MenuItem quitItem = new MenuItem("Quit GSN");
//	         quitItem.addActionListener(quitListener);
//	         popup.addSeparator();
//	         popup.add(quitItem);
//	         
//	         // construct a TrayIcon
//	         trayIcon = new TrayIcon(image, "GSN", popup);
//	         trayIcon.setImageAutoSize(true);
////	         trayIcon.addActionListener(new ActionListener() {
////				public void actionPerformed(ActionEvent arg0) {
////					System.out.println("trayIcon was clicked. visible= " + isVisible());
////					setExtendedState(isVisible()?Frame.ICONIFIED:Frame.NORMAL);
////					setVisible(isVisible()?false:true);
////				}
////	         });
//	         // In the interest of cross-platform compatibility
//	         trayIcon.addMouseListener(new MouseListener() {
//
//				public void mouseClicked(MouseEvent arg0) {
//					setVisible(isVisible()?false:true);
//					setExtendedState(isVisible()?Frame.NORMAL:Frame.ICONIFIED);
//				}
//				public void mouseEntered(MouseEvent arg0) {	}
//				public void mouseExited(MouseEvent arg0) {}
//				public void mousePressed(MouseEvent arg0) {	}
//				public void mouseReleased(MouseEvent arg0) { }
//	         });
//	         trayIcon.setToolTip("GSN Server");
//	         addWindowListener(new WindowAdapter() {
//	     		public void windowDeiconified(WindowEvent arg0) {
//	     			setVisible(true);
//	     			setExtendedState(Frame.NORMAL);
//	    		}
//
//	    		public void windowIconified(WindowEvent arg0) {
//	    			setVisible(false);
//	    			setExtendedState(Frame.ICONIFIED);
//	    		}	        	 
//	         });
//	         try {
//	             tray.add(trayIcon);
//	         } catch (AWTException e) {
//	             System.err.println(e);
//	         }
//		}
//	}

	

	
	private void initEvents() {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeGSNConfigurator();
			}
		});
	}

	private void initComponents() {
		setSize(800, 600);
		contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		vSensorVisualizerPanel = new VSensorVisualizerPanel();
		configuratorPanel.registerInterestInStartStopState(vSensorVisualizerPanel);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add(configuratorPanel.getPanel(), "GSN Configurator");
		tabbedPane.add(vSensorVisualizerPanel.getPanel(), "VSensor Visualizer");
		contentPane.add(tabbedPane);
//		contentPane.add(configuratorPanel.getPanel());
		initMenuBar();
		setJMenuBar(menuBar);
		initStatusBar();
		contentPane.add(statusBar, BorderLayout.SOUTH);

	}

	/**
	 * 
	 */
	private void initStatusBar() {
		statusBar = new GSNStatusBar();
		configuratorPanel.registerInterestInStartStopState(statusBar);
	}

	private ActionListener quitListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			closeGSNConfigurator();
		}
	};
	
	private void initMenuBar() {
		menuBar = new JMenuBar();

		menuFile = new JMenu(MENU_FILE);
//		menuNewConfig = new JMenuItem(MENU_NEWCONFIG);

		menuQuit = new JMenuItem(MENU_QUIT);
		menuQuit.setAccelerator(ACCEL_QUIT);

		menuQuit.addActionListener(quitListener);

		menuFile.setMnemonic(ACCEL_FILE);
//		menuFile.add(menuNewConfig);
		menuFile.add(new JMenuItem(new UpdateAction()));
		menuFile.add(new JMenuItem(new AboutAction(getBackground())));
		menuFile.addSeparator();
		menuFile.add(menuQuit);
		menuBar.add(menuFile);
		menuBar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);

	}

	public static void main(String[] args)  {
		
		ValidityTools.checkAccessibilityOfFiles(Main.DEFAULT_GSN_CONF_FILE,
				Main.DEFAULT_GSN_LOG4J_PROPERTIES);
		ValidityTools
		.checkAccessibilityOfDirs(Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY);
		PlasticLookAndFeel laf = new PlasticXPLookAndFeel();
		PlasticLookAndFeel.setCurrentTheme(new DesertBluer());
		try {
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		
		try {
		new GSNConfiguratorFrame(Main.DEFAULT_GSN_CONF_FILE,
				Main.DEFAULT_GSN_LOG4J_PROPERTIES,
		"conf/log4j.directory.properties");
		} catch(FileNotFoundException exception) {
			System.out.println("Configuration file could not be found ! Stopping now (Error message: " + exception.getMessage());
			exception.getStackTrace();
		} catch(JiBXException exception) {
			System.out.println("GSN Configurator encountered an error. Please report it to the gsn team at http://gsn.sourceforge.net. Error message was: " + exception.getMessage());
			exception.getStackTrace();
			
		}
	}

	/*
	 * This is the central point of exit.
	 */
	private void closeGSNConfigurator() {
		if(configuratorPanel.isGsnRunning()) {
		JOptionPane confirmExitPane = new JOptionPane( 
				"Are you sure you want to exit GSN Control Center ? GSN will be stopped.",
				JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
				GSN_ICON);
		JDialog confirmExitDialog = confirmExitPane.createDialog(null,
		"Please confirm exit request");
		confirmExitDialog.setVisible(true);
		Object selectedValue = confirmExitPane.getValue();
		if (selectedValue != null
				&& ((Integer) selectedValue).intValue() == JOptionPane.OK_OPTION) {
			System.exit(0);
		}
		} else {
			System.exit(0);
		}
	}
}
class UpdateAction extends AbstractAction{
	public UpdateAction() {
		super("Update");
	}
	public void actionPerformed(ActionEvent action) {
		try {
			if (JOptionPane.showConfirmDialog(null, "Do you want to update the GSN ?","Updating",JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
				return;
			AntRunner.blockingAntTaskExecution(null, "update");
			JOptionPane.showMessageDialog(null, "Update done, please restart the GSN now.","Update status",JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class AboutAction extends AbstractAction {
	private static final String HTML_CODE_ABOUT_DIALOG = "<h1>GSN Control Center</h1><br>"
		+ "<p>This software has been developed by "
		+ "the GSN Development Team ( http://gsn.sourceforge.net ).<br>";
	private Color bgColor;
	public AboutAction(Color bgColor) {
		super("About");
		this.bgColor = bgColor;
	}
	public void actionPerformed(ActionEvent e) {
		JTextPane aboutTextPane = new JTextPane();
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setText(HTML_CODE_ABOUT_DIALOG);
		aboutTextPane.setBackground(bgColor);
		JOptionPane aboutPane = new JOptionPane(aboutTextPane,
				JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
				GSNConfiguratorFrame.GSN_ICON);
		JDialog aboutDialog = aboutPane.createDialog(null,
		"About GSN Configurator");
		aboutDialog.setVisible(true);
	}	
}
