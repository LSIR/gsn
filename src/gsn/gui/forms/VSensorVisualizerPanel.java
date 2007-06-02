package gsn.gui.forms;

import gsn.GSNController;
import gsn.Main;
import gsn.beans.Modifications;
import gsn.beans.VSensorConfig;
import gsn.gui.util.GSNDropDownButton;
import gsn.gui.util.GUIUtils;
import gsn.gui.util.SimpleInternalFrame;
import gsn.gui.util.VSensorConfigUtil;
import gsn.gui.util.VSensorIOUtil;
import gsn.gui.vsv.VSVGraphScene;
import gsn.gui.vsv.VSVNodeWidget;
import gsn.utils.graph.Graph;
import gsn.utils.graph.Node;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.mylar.zest.layout.GridLayoutAlgorithm;
import org.eclipse.mylar.zest.layout.HorizontalTreeLayoutAlgorithm;
import org.eclipse.mylar.zest.layout.LayoutAlgorithm;
import org.eclipse.mylar.zest.layout.LayoutStyles;
import org.eclipse.mylar.zest.layout.RadialLayoutAlgorithm;
import org.eclipse.mylar.zest.layout.SpringLayoutAlgorithm;
import org.eclipse.mylar.zest.layout.TreeLayoutAlgorithm;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Utilities;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class VSensorVisualizerPanel implements StartStopEventListener, VSensorGraphListener, ActionListener {
	public static final int CONFIG_STATUS_DISABLED = 1;

	public static final int CONFIG_STATUS_ENABLED = 0;

	public static final int CONFIG_STATUS_ERROR = 2;

	public static final int CONFIG_STATUS_LOADED = 3;

	public static final int CONNECTION_TRY_COUNT = 5;

	public static final int CONNECTION_TRY_INTERVAL = 5000;

	public static final int GRAPH_REFRESH_INTERVAL_DEFAULT = 5;

	public static final ImageIcon VS_DISABLED_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/vs-disabled.png"));

	public static final ImageIcon VS_ERROR_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/vs-error.png"));

	public static final ImageIcon VS_LOADED_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/vs-green.png"));

	public static final ImageIcon VS_NOT_LOADED_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/vs-green-error.png"));

	public static final ImageIcon LIGHT_BUBBLE_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/show.png"));

	public static final ImageIcon GRAY_BUBBLE_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/hide.png"));

	public static ImageIcon VS_EDIT_ICON = new ImageIcon(Utilities.loadImage("gsn/gui/resources/vs-edit.png"));

	private static final String AUTO_REFRESHING_THREAD = "AUTO_REFRESHING_THREAD";

	private static final String DEFAULT_PANEL = "default panel";

	private static final int GRAPH_REFRESH_INTERVAL_MAX = 3600;

	private static final int GRAPH_REFRESH_INTERVAL_MIN = 1;

	private static final int GRAPH_REFRESH_INTERVAL_STEP = 1;

	private static ImageIcon logoIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/GSNLogo.png"));

	private static ImageIcon refreshIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/refresh.png"));

	private static ImageIcon refreshDiskIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/refresh-disk.png"));

	private static ImageIcon newVSensorIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/vs-add.png"));

	private static ImageIcon showAllIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/show-all-vs.png"));

	private static ImageIcon hideAllIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/hide-all-vs.png"));

	private static ImageIcon showDisabledIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/show-all-disabled-vs.png"));

	private static ImageIcon hideDisabledIcon = new ImageIcon(Utilities.loadImage("gsn/gui/resources/hide-all-disabled-vs.png"));

	private static final String USER_REFRESHING_THREAD = "USER_REFRESHING_THREAD";

	private static final String VISUALIZER_PANEL = "visualizer panel";

	public static void main(String[] args) {
		VSensorConfig c1 = new VSensorConfig();
		c1.setName("name");
		VSensorConfig c2 = new VSensorConfig();
		c2.setName("name");
		HashMap<VSensorConfig, Integer> map = new HashMap<VSensorConfig, Integer>();
		map.put(c1, 1);
		System.out.println(map.containsKey(c2));
	}

	private JCheckBox autoRefreshCheckBox;

	private ArrayList<String> vSensorConfigList;

	private ArrayList<Integer> vSensorConfigStatusList;

	private HashMap<String, VSensorConfig> vSensorConfigNameToConfigMap;

	private GSNConnector connector;

	private JPanel defaultPanel;

	private String disabledDir;

	private JScrollPane graphScrollPane;

	private DefaultListModel listModel;

	private JSplitPane mainSplitPane;

	private GSNConnector onDemandConnector;

	private JPanel panel;

	private VSVPopupMenuProvider popupMenuProvider;

	private JButton refreshGraphButton;

	private JButton newVSensorButton;

	private JButton refreshDiskGraphButton;

	private JButton showHideAllButton;

	private JButton showHideDisabledButton;

	private JSpinner refreshIntervalSpinner;

	private SimpleInternalFrame satteliteViewInternalFrame;

	private VSensorGraphScene scene;

	private JComponent sceneSatteliteView;

	private JComponent sceneViewComponent;

	private JPanel visualizerPanel;

	private VSensorIOUtil vSensorIOUtil;

	private JList vsensorJList;

	protected int refreshInterval;

	public VSensorVisualizerPanel() {
		initGui();
		disabledDir = Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY + File.separator + GUIConfig.VSENSOR_DISABLED_DIR_NAME;
		vSensorIOUtil = new VSensorIOUtil(Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY, disabledDir);
		initScene();
		loadVSensorConfigs();
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == autoRefreshCheckBox) {
			if (connector == null) {
				connector = new GSNConnector(AUTO_REFRESHING_THREAD);
				connector.start();
			} else {
				connector.setActive(autoRefreshCheckBox.isSelected());
				if (autoRefreshCheckBox.isSelected()) {
					if (!connector.isConnected() || !connector.isAlive()) {
						connector = new GSNConnector(AUTO_REFRESHING_THREAD);
						connector.start();
					}
				}
			}
		} else if (source == refreshGraphButton && scene != null) {
			if (onDemandConnector == null) {
				onDemandConnector = new GSNConnector(USER_REFRESHING_THREAD);
				onDemandConnector.setActive(false);
			}
			onDemandConnector.connect();
			if (onDemandConnector.isConnected())
				onDemandConnector.getVSensorsFromGSN();
			else
				GUIUtils.showErrorMessage("Error connectiong to GSN");
			onDemandConnector.disconnect();
		} else if (source == refreshDiskGraphButton) {
			loadVSensorConfigs();
			showHideAllButton.setIcon(hideAllIcon);
			showHideDisabledButton.setIcon(hideDisabledIcon);
		} else if (source == newVSensorButton) {
			VSensorConfig vSensorConfig = new VSensorConfig();
			vSensorConfig.setName("please change me_" + Main.randomTableNameGenerator(7));
			VSensorEditor vSensorEditor = new VSensorEditor(vSensorConfig);
			vSensorEditor.open();
			if (!vSensorEditor.hasBeanCanceled()) {
				loadVSensorConfigs();
			}
		} else if (source == showHideAllButton) {
			if (scene != null) {
				boolean condition = showHideAllButton.getIcon() == showAllIcon;
				if (condition) {
					scene.showAll();
					showHideAllButton.setIcon(hideAllIcon);
					showHideDisabledButton.setIcon(hideDisabledIcon);
				} else {
					scene.hideAll();
					showHideAllButton.setIcon(showAllIcon);
				}
				showHideDisabledButton.setEnabled(condition);
				vsensorJList.repaint();
			}
		} else if (source == showHideDisabledButton) {
			if (scene != null) {
				List<Widget> disabledWidgets = getDisabledVSensors();
				if (showHideDisabledButton.getIcon() == showDisabledIcon) {
					scene.showWidgets(disabledWidgets);
					showHideDisabledButton.setIcon(hideDisabledIcon);
				} else {
					scene.hideWidgets(disabledWidgets);
					showHideDisabledButton.setIcon(showDisabledIcon);
				}
				vsensorJList.repaint();
			}
		}
	}

	private List<Widget> getDisabledVSensors() {
		ArrayList<Widget> resultList = new ArrayList<Widget>();
		for (int i = 0; i < vSensorConfigStatusList.size(); i++) {
			if (vSensorConfigStatusList.get(i).intValue() == CONFIG_STATUS_DISABLED) {
				String widgetName = vSensorConfigList.get(i).trim().toLowerCase();
				Widget foundWidget = scene.findWidget(widgetName);
				if (foundWidget != null)
					resultList.add(foundWidget);
			}
		}
		return resultList;
	}

	public JPanel getPanel() {
		return panel;
	}

	public void nodeAdded(VSensorConfig vSensorConfig, Widget widget) {
		synchronized (listModel) {
			listModel.addElement(vSensorConfig);
		}
	}

	public void nodeRemoving(VSensorConfig vSensorConfig, Widget widget) {
		synchronized (listModel) {
			listModel.removeElement(vSensorConfig);
		}

	}

	public void notifyGSNStart() {
		startWatchingGSN();
	}

	public void notifyGSNStop() {
		stopWatchingGSN();
	}

	public void startWatchingGSN() {
		// TODO complete this code
		// ((CardLayout) panel.getLayout()).show(panel, VISUALIZER_PANEL);
		autoRefreshCheckBox.setEnabled(true);
		if (autoRefreshCheckBox.isSelected()) {
			connector = new GSNConnector(AUTO_REFRESHING_THREAD);
			connector.start();
		}
		refreshGraphButton.setEnabled(true);
	}

	public void stopWatchingGSN() {
		// TODO complete this code
		autoRefreshCheckBox.setEnabled(false);
		if (connector != null) {
			connector.interrupt();
			connector = null;
		}
		refreshGraphButton.setEnabled(false);
		// ((CardLayout) panel.getLayout()).show(panel, DEFAULT_PANEL);
		// listModel.clear();
	}

	private void changeSceneLayoutAlgorithm(final LayoutAlgorithm layoutAlgorithm) {
		if (scene != null) {
			synchronized (scene) {
				scene.setLayoutAlgorithm(layoutAlgorithm);
				if (visualizerPanel.isVisible())
					scene.doLayout();
			}
		}
	}

	@SuppressWarnings("serial")
	private JComponent createLayoutDropDwonButton() {
		GSNDropDownButton ddb = new GSNDropDownButton();
		final SpringLayoutAlgorithm springLayoutAlgorithm = new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		springLayoutAlgorithm.setIterations(40);
		final RadialLayoutAlgorithm radialLayoutAlgorithm = new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		final GridLayoutAlgorithm gridLayoutAlgorithm = new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		final TreeLayoutAlgorithm verticalTreeLayoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		final HorizontalTreeLayoutAlgorithm horizontalTreeLayoutAlgorithm = new HorizontalTreeLayoutAlgorithm(
				LayoutStyles.NO_LAYOUT_NODE_RESIZING);

		JMenuItem springLayoutMenuItem = new JMenuItem(new AbstractAction("Spring", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_spring_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				changeSceneLayoutAlgorithm(springLayoutAlgorithm);
			}
		});
		ddb.addMenuItem(springLayoutMenuItem);
		JMenuItem radialLayoutMenuItem = new JMenuItem(new AbstractAction("Radial", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_radial_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				changeSceneLayoutAlgorithm(radialLayoutAlgorithm);
			}
		});
		ddb.addMenuItem(radialLayoutMenuItem);

		JMenuItem gridLayoutMenuItem = new JMenuItem(new AbstractAction("Grid", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_grid_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				changeSceneLayoutAlgorithm(gridLayoutAlgorithm);
			}
		});
		ddb.addMenuItem(gridLayoutMenuItem);

		JMenuItem verticalTreeLayoutMenuItem = new JMenuItem(new AbstractAction("Vertical Tree", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_tree_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				changeSceneLayoutAlgorithm(verticalTreeLayoutAlgorithm);
			}
		});
		ddb.addMenuItem(verticalTreeLayoutMenuItem);

		JMenuItem horizontalTreeLayoutMenuItem = new JMenuItem(new AbstractAction("Horizontal Tree", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_tree_layout_horizontal.gif"))) {
			public void actionPerformed(ActionEvent e) {
				changeSceneLayoutAlgorithm(horizontalTreeLayoutAlgorithm);
			}
		});
		ddb.addMenuItem(horizontalTreeLayoutMenuItem);

		return ddb;
	}

	private Integer getConfigStatus(VSensorConfig sensorConfig) {
		int index = vSensorConfigList.indexOf(sensorConfig.getName());
		if (index != -1)
			return vSensorConfigStatusList.get(index);
		return null;
	}

	private void initGui() {
		listModel = new DefaultListModel();
		vsensorJList = new JList(new SortedListModel(listModel));
		vsensorJList.setCellRenderer(new VSensorListCellRenderer());
		vsensorJList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int index = vsensorJList.locationToIndex(e.getPoint());
					if (scene != null && index > -1) {
						popupMenuProvider.getPopupMenu(scene.findWidget(vsensorJList.getModel().getElementAt(index)), null).show(
								vsensorJList, e.getX(), e.getY());
					}
				}
			}
		});
		SimpleInternalFrame vsensorListFrame = new SimpleInternalFrame("Virtual Sensors");
		vsensorListFrame.setBorder(null);
		JScrollPane listScrollPane = new JScrollPane(vsensorJList);
		listScrollPane.setBorder(null);
		vsensorListFrame.add(listScrollPane);

		satteliteViewInternalFrame = new SimpleInternalFrame("Diagram Overview");
		satteliteViewInternalFrame.setBorder(null);
		satteliteViewInternalFrame.setPreferredSize(new Dimension(150, 100));

		JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		leftSplitPane.setLeftComponent(vsensorListFrame);
		leftSplitPane.setRightComponent(satteliteViewInternalFrame);
		leftSplitPane.setResizeWeight(0.5);
		leftSplitPane.setPreferredSize(new Dimension(150, 100));

		panel = new JPanel(new BorderLayout());
		defaultPanel = new JPanel(new BorderLayout());
		JLabel gsnLogoLabel = new JLabel(logoIcon);
		defaultPanel.add(gsnLogoLabel, BorderLayout.CENTER);
		JLabel messageLabel = new JLabel("Start GSN to activate the visualizer");
		messageLabel.setFont(messageLabel.getFont().deriveFont(20f));
		messageLabel.setHorizontalAlignment(SwingUtilities.CENTER);
		defaultPanel.add(messageLabel, BorderLayout.SOUTH);

		graphScrollPane = new JScrollPane();
		graphScrollPane.setBorder(null);
		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, graphScrollPane);
		mainSplitPane.setResizeWeight(0.15);

		visualizerPanel = new JPanel(new BorderLayout());
		visualizerPanel.add(mainSplitPane, BorderLayout.CENTER);

		JPanel toolBarPanel = new JPanel();
		toolBarPanel.setBorder(BorderFactory.createEtchedBorder());
		refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(GRAPH_REFRESH_INTERVAL_DEFAULT, GRAPH_REFRESH_INTERVAL_MIN,
				GRAPH_REFRESH_INTERVAL_MAX, GRAPH_REFRESH_INTERVAL_STEP));
		refreshIntervalSpinner.setToolTipText("Refresh interval in seconds");
		refreshIntervalSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				try {
					int spinnerValue = Integer.parseInt(String.valueOf(refreshIntervalSpinner.getValue()));
					refreshInterval = spinnerValue;
				} catch (NumberFormatException e1) {
					refreshIntervalSpinner.setValue(refreshInterval);
				}
			}
		});
		refreshInterval = GRAPH_REFRESH_INTERVAL_DEFAULT;

		autoRefreshCheckBox = new JCheckBox("Refresh in selected Interval");
		autoRefreshCheckBox.setSelected(false);
		autoRefreshCheckBox.addActionListener(this);
		autoRefreshCheckBox.setEnabled(false);

		refreshGraphButton = new JButton(refreshIcon);
		refreshGraphButton.setToolTipText("Click to refresh virtual sensors");
		refreshGraphButton.addActionListener(this);
		refreshGraphButton.setEnabled(false);

		refreshDiskGraphButton = new JButton(refreshDiskIcon);
		refreshDiskGraphButton.setToolTipText("Click to reload virtual sensors from disk");
		refreshDiskGraphButton.addActionListener(this);
		refreshDiskGraphButton.setEnabled(true);

		newVSensorButton = new JButton(newVSensorIcon);
		newVSensorButton.setToolTipText("New Virtual Sensor");
		newVSensorButton.addActionListener(this);
		newVSensorButton.setEnabled(true);

		showHideAllButton = new JButton(hideAllIcon);
		showHideAllButton.setToolTipText("Click to show/hide all virtual sensors");
		showHideAllButton.addActionListener(this);

		showHideDisabledButton = new JButton(hideDisabledIcon);
		showHideDisabledButton.setToolTipText("Click to show/hide disabled virtual sensors");
		showHideDisabledButton.addActionListener(this);

		JComponent layoutDropDwonButton = createLayoutDropDwonButton();
		layoutDropDwonButton.setToolTipText("Layout graph");

		FormLayout layout = new FormLayout("2dlu, pref, 2dlu,pref,2dlu,pref,2dlu,pref,8dlu,pref,4dlu,pref,8dlu,pref,2dlu,pref,2dlu",
				"2dlu,p,2dlu");
		CellConstraints cc = new CellConstraints();
		toolBarPanel.setLayout(layout);
		toolBarPanel.add(newVSensorButton, cc.xy(2, 2));
		toolBarPanel.add(layoutDropDwonButton, cc.xy(4, 2));
		toolBarPanel.add(refreshDiskGraphButton, cc.xy(6, 2));
		toolBarPanel.add(refreshGraphButton, cc.xy(8, 2));
		toolBarPanel.add(refreshIntervalSpinner, cc.xy(10, 2));
		toolBarPanel.add(autoRefreshCheckBox, cc.xy(12, 2));
		toolBarPanel.add(showHideAllButton, cc.xy(14, 2));
		toolBarPanel.add(showHideDisabledButton, cc.xy(16, 2));
		visualizerPanel.add(toolBarPanel, BorderLayout.NORTH);

		CardLayout cardLayout = new CardLayout();
		panel.setLayout(cardLayout);
		panel.add(visualizerPanel, VISUALIZER_PANEL);
		panel.add(defaultPanel, DEFAULT_PANEL);
	}

	private void initScene() {
		scene = new VSensorGraphScene(null);
		scene.addChangeListener(VSensorVisualizerPanel.this);
		popupMenuProvider = new VSVPopupMenuProvider(scene);
		scene.setPopupAction(ActionFactory.createPopupMenuAction(popupMenuProvider));
		sceneViewComponent = scene.createView();
		graphScrollPane.setViewportView(sceneViewComponent);

		if (sceneSatteliteView != null)
			satteliteViewInternalFrame.remove(sceneSatteliteView);
		satteliteViewInternalFrame.add(sceneSatteliteView = scene.createSatelliteView());
	}

	private void loadVSensorConfigs() {
		vSensorConfigList = new ArrayList<String>();
		vSensorConfigStatusList = new ArrayList<Integer>();
		vSensorConfigNameToConfigMap = new HashMap<String, VSensorConfig>();

		try {
			File[] enabledVSensorFiles = vSensorIOUtil.readVirtualSensors();
			File[] disabledVSensorFiles = vSensorIOUtil.readDisabledVirtualSensors();
			HashMap<File, VSensorConfig> enabledVSensorMap = VSensorConfigUtil.getVSensorConfigs(enabledVSensorFiles);
			HashMap<File, VSensorConfig> disabledVSensorMap = VSensorConfigUtil.getVSensorConfigs(disabledVSensorFiles);
			HashSet<VSensorConfig> vSensorConfigSet = new HashSet<VSensorConfig>();
			ArrayList<File> badConfigFiles = new ArrayList<File>();

			for (Map.Entry<File, VSensorConfig> entry : enabledVSensorMap.entrySet()) {
				VSensorConfig config = entry.getValue();
				if (config != null && validateVSensorConfig(config)) {
					vSensorConfigSet.add(config);
					putInVSensorConfigList(config, CONFIG_STATUS_ENABLED);
				} else {
					badConfigFiles.add(entry.getKey());
				}
			}

			for (Map.Entry<File, VSensorConfig> entry : disabledVSensorMap.entrySet()) {
				VSensorConfig config = entry.getValue();
				if (config != null && validateVSensorConfig(config)) {
					vSensorConfigSet.add(config);
					putInVSensorConfigList(config, CONFIG_STATUS_DISABLED);
				} else {
					badConfigFiles.add(entry.getKey());
				}
			}

			Graph<VSensorConfig> allVSensorsDepGraph = Modifications.buildDependencyGraphFromIterator(vSensorConfigSet.iterator());
			scene.removeAllNodes();
			listModel.removeAllElements();
			scene.setVSDependencyGraph(allVSensorsDepGraph, false);
			for (String vsName : vSensorConfigList) {
				VSensorConfig config = vSensorConfigNameToConfigMap.get(vsName);
				VSVNodeWidget widget = (VSVNodeWidget) scene.findWidget(config);
				// In the case of remote sources that are not valid, the VS is
				// not added to the graph
				if (widget != null)
					widget.setWidgetIcon(getStatusIcon(config).getImage());
			}
			scene.doLayout();
			if (!badConfigFiles.isEmpty()) {
				StringBuilder stringBuilder = new StringBuilder("<HTML>The following virtsul sensor definition files have error ");
				stringBuilder.append("<OL>");
				for (File file : badConfigFiles) {
					stringBuilder.append("<LI>").append(file.getAbsolutePath()).append("</LI>");
				}
				stringBuilder.append("</OL>");
				stringBuilder.append("</HTML>");
				GUIUtils.showMessage(stringBuilder.toString(), "Virtual Sensor Definition Error");
			}
		} catch (IOException e) {
			GUIUtils.showErrorMessage(e.getMessage());
		}
	}

	private void putInVSensorConfigList(final VSensorConfig config, int status) {
		VSensorConfig oldConfig = null;
		String vsName = config.getName();
		int index = vSensorConfigList.indexOf(vsName);
		if (index != -1) {
			vSensorConfigStatusList.set(index, status);
			oldConfig = vSensorConfigNameToConfigMap.get(vsName);
		} else {
			vSensorConfigList.add(vsName);
			vSensorConfigStatusList.add(status);
		}
		vSensorConfigNameToConfigMap.put(vsName, config);

		// TODO: handle concurrency
		if (scene != null) {
			VSVNodeWidget widget;
			if (oldConfig == null)
				widget = (VSVNodeWidget) scene.findWidget(config);
			else
				widget = (VSVNodeWidget) scene.findWidget(oldConfig);
			if (widget != null) {
				widget.setWidgetIcon(getStatusIcon(config).getImage());
				if (oldConfig != null) {
					scene.removeObject(oldConfig);
					scene.addObject(config, widget);
					int indexInListModel = listModel.indexOf(oldConfig);
					listModel.set(indexInListModel, config);
					widget.objectUpdated();
				}
			}
			//If we don't do this, we might face some NPEs
			scene.validate();
		}
	}

	private void enableDisableVSensor(VSensorConfig vSensorConfig) {
		if (getConfigStatus(vSensorConfig) == CONFIG_STATUS_DISABLED) {
			try {
				vSensorIOUtil.enableVirtualSensor(new File(vSensorConfig.getFileName()));
				loadVSensorConfigs();
			} catch (IOException e) {
				GUIUtils.showErrorMessage(e.getMessage());
			}
		} else {
			try {
				vSensorIOUtil.disableVirtualSensor(new File(vSensorConfig.getFileName()));
				loadVSensorConfigs();
			} catch (IOException e) {
				GUIUtils.showErrorMessage(e.getMessage());
			}
		}
	}

	private void editVSensorConfig(VSensorConfig vSensorConfig) {
		VSensorEditor editor = new VSensorEditor(vSensorConfig);
		editor.open();
		if (!editor.hasBeanCanceled()) {
			loadVSensorConfigs();
		}
	}

	private boolean validateVSensorConfig(VSensorConfig config) {
		for (gsn.beans.InputStream inputStream : config.getInputStreams()) {
			if (!inputStream.validate())
				return false;
		}
		return true;
	}

	public class VSVPopupMenuProvider implements PopupMenuProvider {

		private JPopupMenu popupMenu;

		private VSVGraphScene scene;

		private ShowHideAction showHideAction;

		private EnableDisableAction enableDisableAction;

		private ModifyVSensorAction modifyVSensorAction;

		public VSVPopupMenuProvider(VSVGraphScene scene) {
			this.scene = scene;

			popupMenu = new JPopupMenu();
			JMenuItem showHideMenuItem = new JMenuItem();
			showHideAction = new ShowHideAction();
			showHideMenuItem.setAction(showHideAction);
			popupMenu.add(showHideMenuItem);

			JMenuItem enableDisableMenuItem = new JMenuItem();
			enableDisableAction = new EnableDisableAction();
			enableDisableMenuItem.setAction(enableDisableAction);
			popupMenu.add(enableDisableMenuItem);

			JMenuItem editMenuItem = new JMenuItem();
			modifyVSensorAction = new ModifyVSensorAction();
			editMenuItem.setAction(modifyVSensorAction);
			popupMenu.add(editMenuItem);

		}

		public JPopupMenu getPopupMenu(final Widget widget, Point localLocation) {
			showHideAction.setWidget(widget);
			enableDisableAction.setWidget(widget);
			modifyVSensorAction.setWidget(widget);
			return popupMenu;
		}

	}

	private class GSNConnector extends Thread {
		private static final String DUMMY_MESSAGE = "Dummy Message";

		boolean active;

		boolean connected;

		InputStream inputStream;

		ObjectInputStream objInputStream = null;

		Socket socket;

		PrintWriter writer;

		public GSNConnector(String name) {
			setName(name);
			active = true;
			connected = false;
		}

		public void connect() {
			socket = new Socket();
			for (int i = 0; i < CONNECTION_TRY_COUNT; i++) {
				try {
					Thread.sleep(CONNECTION_TRY_INTERVAL);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				try {
					System.out.println(getName() + ": " + (i + 1) + ". Trying to connect to GSN at " + InetAddress.getLocalHost() + ":"
							+ GSNController.GSN_CONTROL_PORT);
					if (socket.isClosed())
						socket = new Socket();
					socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), GSNController.GSN_CONTROL_PORT));
					writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
					inputStream = socket.getInputStream();
					connected = true;
					System.out.println(getName() + ": Connected");
					return;
				} catch (Exception e) {
					System.out.println(getName() + ": Error connecting to GSN : " + e.getMessage());
				}
			}
			System.out.println(getName() + ": Could not connet to GSN after " + CONNECTION_TRY_COUNT + " tries");
		}

		public void disconnect() {
			if (connected) {
				System.out.println(getName() + ": Closing connection to GSN...");
				writer.close();
				try {
					socket.close();
					objInputStream.close();
				} catch (IOException exx) {
					exx.printStackTrace();
				}
				connected = false;
			}
		}

		public boolean isConnected() {
			return connected;
		}

		public void run() {
			if (connected == false)
				connect();
			while (connected && !isInterrupted()) {
				if (active)
					getVSensorsFromGSN();
				try {
					for (int i = 0; connected && i < refreshInterval / GRAPH_REFRESH_INTERVAL_DEFAULT; i++) {
						Thread.sleep(GRAPH_REFRESH_INTERVAL_DEFAULT * 1000);
						sendDummyMessage();
					}
					int sleepTime = (refreshInterval % GRAPH_REFRESH_INTERVAL_DEFAULT) * 1000;
					Thread.sleep(sleepTime);
				} catch (InterruptedException ex) {
					disconnect();
				}
			}

		}

		public void setActive(boolean active) {
			this.active = active;
		}

		private synchronized void getVSensorsFromGSN() {
			if (connected == false)
				return;
			System.out.println(getName() + ": Sending command to gsn : " + gsn.GSNController.GSN_CONTROL_LIST_LOADED_VSENSORS);
			writer.println(gsn.GSNController.GSN_CONTROL_LIST_LOADED_VSENSORS);
			writer.flush();
			try {
				objInputStream = new ObjectInputStream(inputStream);
				Object obj = objInputStream.readObject();
				if (obj instanceof Graph) {
					final Graph<VSensorConfig> graph = (Graph<VSensorConfig>) obj;
					if (scene.runningGraphModified(graph)) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// scene.setVSDependencyGraph(graph, false);
								// scene.doLayout();
								ArrayList<Node<VSensorConfig>> nodes = graph.getNodes();
								for (Node<VSensorConfig> node : nodes) {
									putInVSensorConfigList(node.getObject(), CONFIG_STATUS_LOADED);
								}
								panel.repaint();
							}
						});
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		}

		private void sendDummyMessage() {
			if (connected) {
				writer.println(DUMMY_MESSAGE);
				writer.flush();
			}
		}

	}

	private class RefreshGraphDialog extends JDialog {
		JLabel label;

		public RefreshGraphDialog(Window owner, ModalityType modalityType) {
			super(owner, modalityType);
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			label = new JLabel("Virtual sensor modification was detected. Refreshing Visualizer...");
			label.setFont(label.getFont().deriveFont(20f));
			add(label);
		}

		public void showDialog(Graph<VSensorConfig> graph) {
			pack();
			setLocationByPlatform(true);

		}
	}

	private class ShowHideAction extends AbstractAction {
		private Widget widget;

		public ShowHideAction() {

		}

		public void actionPerformed(ActionEvent e) {
			if (widget != null) {
				scene.changeVisibility((VSVNodeWidget) widget);
				vsensorJList.repaint();
			}
		}

		public void setWidget(Widget widget) {
			this.widget = widget;
			putValue(NAME, (widget.isVisible() ? "Hide " : "Show ") + ((VSVNodeWidget) widget).getNodeName());
			putValue(SMALL_ICON, (widget.isVisible() ? GRAY_BUBBLE_ICON : LIGHT_BUBBLE_ICON));
		}
	}

	private class EnableDisableAction extends AbstractAction {
		private Widget widget;

		public EnableDisableAction() {

		}

		public void actionPerformed(ActionEvent e) {
			if (widget != null) {
				VSensorConfig config = (VSensorConfig) scene.findObject(widget);
				enableDisableVSensor(config);
				panel.repaint();
			}
		}

		public void setWidget(Widget widget) {
			this.widget = widget;
			VSensorConfig config = (VSensorConfig) scene.findObject(widget);
			if (getConfigStatus(config) == CONFIG_STATUS_DISABLED) {
				putValue(NAME, "Enable " + ((VSVNodeWidget) widget).getNodeName());
				putValue(SMALL_ICON, VS_LOADED_ICON);
			} else {
				putValue(NAME, "Disable " + ((VSVNodeWidget) widget).getNodeName());
				putValue(SMALL_ICON, VS_DISABLED_ICON);
			}
		}
	}

	private class ModifyVSensorAction extends AbstractAction {
		private Widget widget;

		public ModifyVSensorAction() {
			putValue(SMALL_ICON, VS_EDIT_ICON);
		}

		public void actionPerformed(ActionEvent e) {
			if (widget != null) {
				VSensorConfig config = (VSensorConfig) scene.findObject(widget);
				editVSensorConfig(config);
			}
		}

		public void setWidget(Widget widget) {
			this.widget = widget;
			// VSensorConfig config = (VSensorConfig) scene.findObject(widget);
			putValue(NAME, "Edit " + ((VSVNodeWidget) widget).getNodeName());
		}
	}

	@SuppressWarnings("serial")
	private class VSensorListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel defaultRenderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof VSensorConfig) {
				VSensorConfig vSensorConfig = (VSensorConfig) value;
				defaultRenderer.setText(vSensorConfig.getName().trim().toLowerCase());
				Widget widget = scene.findWidget(defaultRenderer.getText());
				// TODO: widget could be null
				if (widget != null && !widget.isVisible())
					defaultRenderer.setFont(list.getFont());
				else
					defaultRenderer.setFont(list.getFont().deriveFont(Font.ITALIC + Font.BOLD));
				Icon icon = getStatusIcon(vSensorConfig);
				defaultRenderer.setIcon(icon);
			}
			return defaultRenderer;
		}

	}

	private ImageIcon getStatusIcon(VSensorConfig sensorConfig) {
		Integer configStatus = getConfigStatus(sensorConfig);

		if (configStatus == null) {
			return VS_ERROR_ICON;
		}
		if (configStatus.intValue() == CONFIG_STATUS_DISABLED)
			return VS_DISABLED_ICON;
		else if (configStatus.intValue() == CONFIG_STATUS_ENABLED)
			return VS_NOT_LOADED_ICON;
		else if (configStatus.intValue() == CONFIG_STATUS_LOADED)
			return VS_LOADED_ICON;
		else
			return VS_ERROR_ICON;
	}
}
