package gsn.gui.forms;

import gsn.beans.ContainerConfig;
import gsn.gui.AntRunner;
import gsn.utils.ValidityTools;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

public class GSNConfiguratorPanel {

    private static final int LOCK_FILE_POLLING_INTERVAL = 500; // in
                                                                // milliseconds

    private ContainerConfig bean;

    private JPanel panel;

    private LogPanel logPanel;

    private PresentationModel beanPresentationModel;

    private static GSNConfiguratorPanel instance;

    private Vector<StartStopEventListener> listeners = new Vector<StartStopEventListener>();

    private static Icon startIcon;

    private static Icon stopIcon;
    static {
	try {
	    startIcon = new ImageIcon(ImageIO.read(new File("icons/run.gif")));
	    stopIcon = new ImageIcon(ImageIO.read(new File("icons/stop.gif")));
	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    private JButton directoryServiceStartStop = new JButton(startIcon);

    private JButton gsnStart = new JButton(startIcon);

    private JComboBox networkInterface;

    private ValueModel directoryHostAddress;

    private Timer timer;

    private StatusMonitor statusMonitor;
    
   private JFormattedTextField gsnPortNo;

    public GSNConfiguratorPanel(ContainerConfig bean) {
	if (bean == null)
	    throw new NullPointerException("The input bean shoudn't be null.");
	this.bean = bean;
	beanPresentationModel = new PresentationModel(bean);
	directoryHostAddress = beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_registryBootstrapAddr);
	initGUI();
	initEvents();
	statusMonitor = new StatusMonitor();
	timer = new Timer(LOCK_FILE_POLLING_INTERVAL, statusMonitor);
	timer.start();
	instance = this;
    }
    

    /*
         * This class constantly monitors for the presence of the pid files.
         * When it detects a change, it propagates the information to all
         * interested parties (currently: JButton, statusbar). It is also used
         * to instantiate and to stop GSN and the directory service. When one or
         * both of these services are running, the log display is periodically
         * updated.
         */
    class StatusMonitor implements ActionListener {
	private boolean gsnIsRunning = false, dirIsRunning = false;

	private BufferedReader dirReader, gsnReader;

	public void startGsn() {

	    try {
		bean.writeConfigurations();
		try {
		    AntRunner.nonBlockingAntTaskExecution(null, "gsn");
		    notifyGsnHasStarted();
		} catch (Exception e1) {
		    e1.printStackTrace();
		}
	    } catch (FileNotFoundException e1) {
		confirmConstraint(false,
			"Can't write the configuration to the file ("
				+ e1.getMessage() + ")");
		return;
	    } catch (IOException e1) {
		confirmConstraint(false,
			"Can't write the configuration to the file ("
				+ e1.getMessage() + ")");
		return;
	    }
	}

	public void stopGsn() {
//           FIXME
//	    try {
//		PIDUtils.killPID(new File(System.getProperty("java.io.tmpdir")
//			+ "/" + PIDUtils.GSN_PID));
//	    } catch (IOException e1) {
//		e1.printStackTrace();
//	    }
	}

	public void startDir() {
	    try {
		bean.setRegistryBootstrapAddr(networkInterface.getEditor()
			.getItem().toString());
		bean.writeConfigurations();
		logPanel.getDirLogView().clear();

		try {
		    Map<String, String> properties = new HashMap<String, String>();
		    properties.put("port", bean.extractDirectoryServicePort()
			    + "");
		    properties.put("host", bean.extractDirectoryServiceHost());
		    AntRunner.nonBlockingAntTaskExecution(properties, "dir");
		    notifyDirHasStarted();

		} catch (Exception e1) {
		    e1.printStackTrace();
		}
	    } catch (FileNotFoundException e1) {
		confirmConstraint(false,
			"Can't write the configuration to the file ("
				+ e1.getMessage() + ")");
		return;
	    } catch (IOException e1) {
		confirmConstraint(false,
			"Can't write the configuration to the file ("
				+ e1.getMessage() + ")");
		return;
	    }
	}

	public void stopDir() {
           // Fixme
//	    try {
//		PIDUtils.killPID(new File(System.getProperty("java.io.tmpdir")
//			+ "/" + PIDUtils.DIRECTORY_SERVICE_PID));
//	    } catch (IOException e1) {
//		e1.printStackTrace();
//	    }
	}

	public void actionPerformed(ActionEvent e) {
	    updateGsnLogView();
	    updateDirLogView();
	    checkGsnStatus();
	    checkDirStatus();
	}

	private void checkDirStatus() {
	   // FIXME 
//           if (PIDUtils.isPIDExist(gsn.pid.PIDUtils.DIRECTORY_SERVICE_PID) != dirIsRunning) {
//		if (dirIsRunning) {
//		    notifyDirHasStopped();
//		} else {
//		    notifyDirHasStarted();
//		}
//	    }
	}

	private void checkGsnStatus() {
	   // FIXME
//           if (PIDUtils.isPIDExist(PIDUtils.GSN_PID) != gsnIsRunning) {
//		if (gsnIsRunning) {
//		    notifyGsnHasStopped();
//		} else {
//		    notifyGsnHasStarted();
//		}
//	    }
	}

	private void updateDirLogView() {
	    if (dirIsRunning) {
		try {
		    if (dirReader == null) {
			try {
			    dirReader = new BufferedReader(new FileReader(
				    LogPanel.DIR_LOG_FILE));
			} catch (FileNotFoundException ex) {
			}
		    }
		    if (dirReader != null && dirReader.ready()) {
			Vector<String> data = new Vector<String>();

			while (dirReader.ready()) {
			    String s = dirReader.readLine();
			    data.add(s);
			}
			logPanel.getDirLogView().doLog(data);
		    }
		} catch (IOException ioe) {
		}
	    }
	}

	/**
         * 
         */
	private void updateGsnLogView() {
	    if (gsnIsRunning) {
		try {
		    if (gsnReader == null) {
			try {
			    gsnReader = new BufferedReader(new FileReader(
				    LogPanel.GSN_LOG_FILE));
			} catch (FileNotFoundException exc) {
			    ;
			}
		    }
		    if (gsnReader != null && gsnReader.ready()) {
			Vector<String> data = new Vector<String>();

			while (gsnReader.ready()) {
			    String s = gsnReader.readLine();
			    data.add(s);
			}
			logPanel.getGSNLogView().doLog(data);
		    }

		} catch (IOException ioe) {
		    ;// System.out.println("Error while reading log file " +
                        // log_file + ": " + ioe);
		}
	    }
	}

	public boolean isGsnRunning() {
	    return gsnIsRunning;
	}

	public boolean isDirRunning() {
	    return dirIsRunning;
	}

	public void notifyGsnHasStarted() {
	    logPanel.getGSNLogView().clear();
	    gsnIsRunning = true;
	    gsnStart.setIcon(stopIcon);
	    for (StartStopEventListener listener : listeners)
		listener.notifyGSNStart();
//	    JOptionPane.showMessageDialog(null, "Congratulations! GSN has successfully started. You can use the web interface by visiting http://localhost:1882/index.jsp with your web browser.");
	}

	public void notifyGsnHasStopped() {
	    gsnIsRunning = false;
	    gsnStart.setIcon(startIcon);
	    if (gsnReader != null) {
		try {
		    gsnReader.close();
		} catch (IOException ioe) {
		    System.out.println(ioe.getMessage());
		}
		gsnReader = null;
	    }
	    for (StartStopEventListener listener : listeners)
		listener.notifyGSNStop();
	}

	public void notifyDirHasStarted() {
	    logPanel.getDirLogView().clear();
	    dirIsRunning = true;
	    directoryServiceStartStop.setIcon(stopIcon);
	    if (dirReader != null) {
		try {
		    dirReader.close();
		} catch (IOException ioe) {
		    System.out.println(ioe.getMessage());
		}

		dirReader = null;
	    }
	    for (StartStopEventListener listener : listeners)
		listener.notifyGSNDirStart();
	}

	public void notifyDirHasStopped() {
	    dirIsRunning = false;
	    directoryServiceStartStop.setIcon(startIcon);

	    for (StartStopEventListener listener : listeners)
		listener.notifyGSNDirStop();
	}
    }

    /**
         * Returns null if there is this class is not yet constructed.
         * 
         * @return
         */
    public static GSNConfiguratorPanel getInstance() {
	return instance;
    }

    private void initEvents() {
	directoryServiceStartStop.addActionListener(new ActionListener() {

	    public boolean testBeforeRunningDirectory() {
               //FIXME
//		if (!confirmConstraint(!PIDUtils
//			.isPIDExist(PIDUtils.DIRECTORY_SERVICE_PID),
//			"Another GSN Directory Server is already running on this machine."))
//		    return false;
               
		String dirHost = bean
			.extractDirectoryServiceHost((String) networkInterface
				.getSelectedItem());
		if (!confirmConstraint(
			(dirHost != null && !dirHost.equals("")),
			"The specified host name for the directory server is not valid.("
				+ dirHost + ")."))
		    return false;
		dirHost = dirHost.trim();
		int dirPort = bean.extractDirectoryServicePort();
		if (!confirmConstraint((dirPort > 0 || dirPort < 65000),
			"The specified port number for the directory server is not valid.("
				+ dirPort + ")"))
		    return false;
		if (!confirmConstraint(
			(bean.getMaxDirectoryLogSizeInMB() > 0 && bean
				.getMaxDirectoryLogSizeInMB() <= 100),
			"The size of the log file is limited to 100 MB. (Min is 1MB)"))
		    return false;

		try {
		    boolean isLocalHost = ValidityTools.isLocalhost(dirHost);
		    if (isLocalHost) {
			if (!confirmConstraint(!ValidityTools
				.isAccessibleSocket(dirHost, dirPort),
				"The specified port number on the local machine is busy."))
			    return false;
		    } else {
			if (!confirmConstraint(ValidityTools
				.isAccessibleSocket(dirHost, dirPort),
				"There is no directory service running at the specified host and port pairs."))
			    return false;
			confirmConstraint(false,
				"The specified address doesn't point to this machine.");
			return false;
		    }
		} catch (Exception e) {
		    return confirmConstraint(false,
			    "The specified directory address doesn't point to this machine.("
				    + e.getMessage() + ")");
		}
		return true;
	    }

	    public void actionPerformed(ActionEvent e) {
		if (statusMonitor.isDirRunning() == false) {
		    if (testBeforeRunningDirectory() == false)
			return;
		    statusMonitor.startDir();
		} else {
		    statusMonitor.stopDir();
		}
	    }
	});

	gsnStart.addActionListener(new ActionListener() {

	    public boolean testBeforeRunningDirectory() {
               // FIXME
//		if (!confirmConstraint(!PIDUtils.isPIDExist(PIDUtils.GSN_PID),
//			"Another GSN Server is running."))
//		    return false;

		int gsnPort = bean.getContainerPort();
		if (!confirmConstraint((gsnPort > 0 || gsnPort < 65000),
			"The specified port number for GSN is not valid."))
		    return false;
		if (!confirmConstraint((bean.getMaxGSNLogSizeInMB() > 0 && bean
			.getMaxGSNLogSizeInMB() <= 100),
			"The size of the log file is limited to 100 MB. (Min is 1MB)"))
		    return false;
		try {
		    if (!confirmConstraint(!ValidityTools.isAccessibleSocket(
			    "localhost", gsnPort),
			    "The specified port number<br>for the GSN is busy."))
			return false;
		} catch (Exception e2) {
		    confirmConstraint(false,
			    "The specified port number<br>for the GSN is busy.("
				    + e2.getMessage() + ")");
		    return false;
		}
		try {
		    if (!confirmConstraint(ValidityTools.isAccessibleSocket(
			    bean.extractDirectoryServiceHost(), bean
				    .extractDirectoryServicePort()),
			    "The specified directory server is not accessible."))
			return false;
		} catch (Exception e2) {
		    confirmConstraint(false,
			    "The specified port number<br>for the GSN is busy.("
				    + e2.getMessage() + ")");
		    return false;
		}
		try {
		    ValidityTools.isDBAccessible(bean.getJdbcDriver(), bean
			    .getJdbcURL(), bean.getJdbcUsername(), bean
			    .getJdbcPassword());
		} catch (Exception e1) {
		    confirmConstraint(false,
			    "Can't connect to the specified database server ("
				    + e1.getMessage() + ")");
		    return false;
		}
		return true;
	    }

	    public void actionPerformed(ActionEvent e) {
		if (statusMonitor.isGsnRunning() == false) {
		    if (testBeforeRunningDirectory() == false)
			return;
		    statusMonitor.startGsn();
		} else {
		    statusMonitor.stopGsn();
		}
	    }
	});
    }

    private boolean confirmConstraint(boolean validitiyCondition, String message) {
	if (!validitiyCondition) {
	    JOptionPane.showMessageDialog(null, "<html><center>" + message,
		    "Error", JOptionPane.ERROR_MESSAGE);
	    return false;
	}
	return true;

    }

    private void initGUI() {
	panel = new JPanel(new BorderLayout());
	panel.add(initPersonalDataPanel(), BorderLayout.NORTH);
	JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	splitPane.setTopComponent(initGeneralConfigurationPanel());
	splitPane.setBottomComponent(logAppenderPanel());
	panel.add(splitPane);
    }

    /**
         * @return
         */
    private LogPanel logAppenderPanel() {
	logPanel = new LogPanel();
	return logPanel;
    }

    private JPanel initGeneralConfigurationPanel() {
	DefaultFormBuilder panelBuilder = new DefaultFormBuilder(
		new FormLayout("r:pref,4dlu,pref:g,8dlu,r:pref,4dlu,pref:g", ""));
	panelBuilder.setDefaultDialogBorder();
	panelBuilder.appendSeparator("Storage Configuration");
	JComboBox combo;

	combo = BasicComponentFactory
		.createComboBox(new SelectionInList(
         ContainerConfig.JDBC_SYSTEMS,
			beanPresentationModel
				.getModel(ContainerConfig.FIELD_NAME_databaseSystem)));
	combo
		.setToolTipText("GSN needs a database to store information. It is safe to use the default. Use MySQL on a separate server for high performance.");
	panelBuilder.append("Storage", combo);

	JTextField textfield;
	textfield = BasicComponentFactory.createTextField(beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_jdbcUsername));
	textfield
		.setToolTipText("The database user name that gsn should use to store its data.");
	panelBuilder.append("User", textfield);

	textfield = BasicComponentFactory.createTextField(beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_jdbcURL));
	textfield
		.setToolTipText("Enter here a JDBC URL to help gsn find the server.");
	panelBuilder.append("URL", textfield);

	textfield = BasicComponentFactory.createTextField(beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_jdbcPassword));
	textfield.setToolTipText("The password of the database user to use.");
	panelBuilder.append("Password", textfield);
	panelBuilder.appendSeparator("Execution");
	panelBuilder.append("Directory Server",
		networkInterface = new JComboBox(
         ContainerConfig.NETWORK_ADDRESSES));
	networkInterface.setEditable(true);
	networkInterface
		.setToolTipText("Choose here on which IP address should the directory server be running if you intend to run one locally. You can also enter a remote gsn directory server address.");
	networkInterface.setSelectedItem(directoryHostAddress.getValue());

	panelBuilder.append("", ButtonBarFactory
		.buildRightAlignedBar(directoryServiceStartStop));
	directoryServiceStartStop
		.setToolTipText("Use this button to start/stop a local directory server.");
	panelBuilder
		.append(
			"GSN Server Port",
			gsnPortNo = BasicComponentFactory
				.createIntegerField(beanPresentationModel
					.getModel(ContainerConfig.FIELD_NAME_gsnPortNo)));
	textfield
		.setToolTipText("You can change here the port on which your GSN server should run.\n Use this if the port is already in use. Otherwise the default is safe.");
	panelBuilder
		.append("", ButtonBarFactory.buildRightAlignedBar(gsnStart));
	gsnStart.setToolTipText("Start and stop your GSN server");
	panelBuilder.appendSeparator("Logging");
	panelBuilder
		.append(
			"GSN Log Level",
			combo = BasicComponentFactory
				.createComboBox(new SelectionInList(
               ContainerConfig.LOGGING_LEVELS,
					beanPresentationModel
						.getModel(ContainerConfig.FIELD_NAME_gsnLoggingLevel))));
	combo
		.setToolTipText("Choose the level of verbosity for GSN log output. Use DEBUG or INFO to troubleshoot, and WARN for normal operation.");
	panelBuilder
		.append(
			"GSN Log Size (MB)",
			textfield = BasicComponentFactory
				.createIntegerField(
					beanPresentationModel
						.getModel(ContainerConfig.FIELD_NAME_maxGSNLogSizeInMB),
					1));
	textfield
		.setToolTipText("Set here the maximum allowed size of GSN log file.");
	panelBuilder
		.append(
			"Directory Log Level",
			combo = BasicComponentFactory
				.createComboBox(new SelectionInList(
               ContainerConfig.LOGGING_LEVELS,
					beanPresentationModel
						.getModel(ContainerConfig.FIELD_NAME_directoryLoggingLevel))));
	combo
		.setToolTipText("Choose the level of verbosity for GSN Directory service log output. Use DEBUG or INFO to troubleshoot, and WARN for normal operation.");
	panelBuilder
		.append(
			"Directory Log Size (MB)",
			textfield = BasicComponentFactory
				.createIntegerField(
					beanPresentationModel
						.getModel(ContainerConfig.FIELD_NAME_maxDirectoryLogSizeInMB),
					1));
	textfield
		.setToolTipText("Set here the maximum allowed size of GSN Directory log file.");
	return panelBuilder.getPanel();
    }

    private JPanel initPersonalDataPanel() {
	JTextField textfield;

	DefaultFormBuilder panelBuilder = new DefaultFormBuilder(
		new FormLayout("r:pref,4dlu,pref:g,8dlu,r:pref,4dlu,pref:g", ""));
	panelBuilder.setDefaultDialogBorder();
	panelBuilder.appendSeparator("Web Interface Details");

	textfield = BasicComponentFactory.createTextField(beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_webName));
	textfield.setToolTipText("Please choose a name for your server.");
	panelBuilder.append("Server Name", textfield);

	textfield = BasicComponentFactory.createTextField(beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_webAuthor));
	textfield.setToolTipText("You can enter your name here.");
	panelBuilder.append("Author", textfield);

	textfield = BasicComponentFactory.createTextField(beanPresentationModel
		.getModel(ContainerConfig.FIELD_NAME_webEmail));
	textfield.setToolTipText("Enter your email address.");
	panelBuilder.append("Email", textfield);
	return panelBuilder.getPanel();
    }

    public JPanel getPanel() {
	return panel;
    }

    public LogPanel getLogPanel() {
	return logPanel;
    }

    public void registerInterestInStartStopState(StartStopEventListener listener) {
	listeners.add(listener);
    }

    public void removeInterestInStartStopState(StartStopEventListener listener) {
	listeners.remove(listener);
    }

   
   public String getDirectoryHostAddress ( ) {
      return ( String ) directoryHostAddress.getValue( );
   }
   public String getGsnPortNo ( ) {
      return beanPresentationModel.getModel(ContainerConfig.FIELD_NAME_gsnPortNo).getValue( ).toString( );
   }
}
