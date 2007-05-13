package gsn.gui.forms;

import gsn.beans.AddressBean;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.gui.vsv.VSVConnectionWidget;
import gsn.gui.vsv.VSVGraphScene;
import gsn.gui.vsv.VSVNodeWidget;
import gsn.gui.vsv.VSVPinWidget;
import gsn.others.visualization.svg.SVGUtils;
import gsn.utils.graph.Edge;
import gsn.utils.graph.Graph;
import gsn.utils.graph.Node;

import java.awt.Image;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.mylar.zest.layout.LayoutAlgorithm;
import org.eclipse.mylar.zest.layout.LayoutEntity;
import org.eclipse.mylar.zest.layout.LayoutRelationship;
import org.eclipse.mylar.zest.layout.LayoutStyles;
import org.eclipse.mylar.zest.layout.SpringLayoutAlgorithm;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;


/**
 * @author Mehdi Riahi
 * 
 */
public class VSensorGraphScene extends VSVGraphScene {

	public static final String TYPE_STREAM_SOURCE = "source";

	public static final String TYPE_INPUT_STREAM = "stream";

	public static final String TYPE_VIRTUAL_SENSOR = "vsensor";

	private Graph<VSensorConfig> oldRunningGraph;
	
	private Graph<VSensorConfig> oldOnDiskGraph;

	private ArrayList<VSensorGraphListener> listeners;

	private HashMap<String, VSVNodeWidget> nodeIDToNodeWidgetMap;

	private WidgetAction popupAction;

	private LayoutAlgorithm layoutAlgorithm;

	public VSensorGraphScene(Graph<VSensorConfig> vsDependencyGraph) {
		listeners = new ArrayList<VSensorGraphListener>();
		nodeIDToNodeWidgetMap = new HashMap<String, VSVNodeWidget>();
//		oldRunningGraph = vsDependencyGraph;
		oldOnDiskGraph = vsDependencyGraph;

		SpringLayoutAlgorithm springLayoutAlgorithm = new SpringLayoutAlgorithm(LayoutStyles.NONE);
		springLayoutAlgorithm.setIterations(20);
		layoutAlgorithm = springLayoutAlgorithm;

		if (vsDependencyGraph != null)
			createSceneFromVSDependencyGraph(vsDependencyGraph);
	}

	private synchronized void createSceneFromVSDependencyGraph(Graph<VSensorConfig> vsDependencyGraph) {
		setVisible(false);
		removeAllNodes();

		List<Node<VSensorConfig>> allNodes = vsDependencyGraph.getNodes();
		System.out.println("Receiving " + allNodes.size() + " virtual sensor from GSN");
		for (Node<VSensorConfig> node : allNodes) {
			VSensorConfig vsensorConfig = node.getObject();
			String nodeName = createNode(this, (int) (Math.random() * 600), (int) (Math.random() * 600), null, vsensorConfig,
					TYPE_VIRTUAL_SENSOR, null);

			// adding each input stream to the virtual sensor node
			// TODO: add addressings to the node
			HashMap<InputStream, List<Widget>> inputStreamToPinWidgetMap = new HashMap<InputStream, List<Widget>>();
			ArrayList<Widget> widgets = new ArrayList<Widget>();
			Collection<InputStream> inputStreams = vsensorConfig.getInputStreams();
			for (InputStream stream : inputStreams) {
				widgets = new ArrayList<Widget>();
				StreamSource[] sources = stream.getSources();
				for (int i = 0; i < sources.length; i++) {
					String uidStr = sources[i].getUIDStr().toString();
					Widget widget = createPin(this, nodeName, uidStr, null, sources[i].getAlias().toString(), TYPE_STREAM_SOURCE);
					if (widget != null)
						widgets.add(widget);
				}
				inputStreamToPinWidgetMap.put(stream, widgets);
			}
			((VSVNodeWidget) findWidget(nodeName)).addPins(this, inputStreamToPinWidgetMap);
		}

		// creating edges between vsensor nodes
		for (Node<VSensorConfig> node : allNodes) {
			ArrayList<Edge<VSensorConfig>> outputEdges = node.getOutputEdges();
			for (Edge<VSensorConfig> edge : outputEdges) {
				Node<VSensorConfig> endNode = edge.getEndNode();
				createEdge(this, node, endNode);
			}
		}

		// minimize all vsensor nodes
		for (LayoutEntity entity : entities) {
			((VSVNodeWidget) entity).setMinimized(true);
		}

		// doLayout();
	}

	public void doLayout() {
		setVisible(true);

		// computes placement of nodes in the scene
		SVGUtils.performLayout(entities, relationShips, layoutAlgorithm, (int) getBounds().getWidth(), (int) getBounds().getHeight());

		// moves nodes to the locations computed by previous layout algorithm
		layoutScene();
//		getSceneAnimator().animateZoomFactor(1); // to show the scene immediately
		validate();
	}

	/**
	 * Creates edges between two vsensor node
	 * 
	 * @param scene
	 * @param startNode
	 * @param endNode
	 */
	private void createEdge(VSVGraphScene scene, Node<VSensorConfig> startNode, Node<VSensorConfig> endNode) {
		String[] sourcePinIDs = findPinIDs(startNode, endNode);
		String targetNodeID = endNode.getObject().getName().toLowerCase().trim();
		for (int i = 0; i < sourcePinIDs.length; i++) {
			String edgeID = sourcePinIDs[i] + "-" + targetNodeID;
			scene.addEdge(edgeID);
			scene.setEdgeTarget(edgeID, sourcePinIDs[i]);
			scene.setEdgeSource(edgeID, targetNodeID + VSVGraphScene.PIN_ID_DEFAULT_SUFFIX);
		}

	}

	/**
	 * Creates a pin with <code>pinID</code> for vsensor node specified by
	 * <code>nodeID</code>
	 * 
	 * @return created VSVPinWidget
	 */
	private Widget createPin(VSVGraphScene scene, String nodeID, String pinID, Image image, String name, String type) {
		Widget widget = scene.addPin(nodeID, pinID);
		((VSVPinWidget) widget).setProperties(name, null);
		return widget;
	}

	/**
	 * Creates a new VSVNodeWidget with <code>name</code> as nodeID and
	 * associates <code>vsConfig</code> to the created widget in the scene
	 * 
	 * @return nodeID
	 */
	private String createNode(VSVGraphScene scene, int x, int y, Image image, VSensorConfig vsConfig, String type,
			java.util.List<Image> glyphs) {
		String name = vsConfig.getName().toLowerCase().trim();
		VSVNodeWidget widget = (VSVNodeWidget) scene.addNode(name);
		// widget.setMinimized(true);
		widget.setPreferredLocation(new Point(x, y));
		widget.setNodeProperties(image, name, type, glyphs);
		widget.setNodeType(null);
		widget.getActions().addAction(popupAction);
		scene.addObject(vsConfig, widget);

		// creating a pin in order to connect edges to this pin
		scene.addPin(name, name + VSVGraphScene.PIN_ID_DEFAULT_SUFFIX);
		widget.setOpaque(true);
		widget.addDetails();
		// adding new widget to entity list
		entities.add(widget);
		nodeIDToNodeWidgetMap.put(name, widget);
		// notifying all listeners
		for (VSensorGraphListener listener : listeners) {
			listener.nodeAdded(vsConfig, widget);
		}
		return name;
	}

	private String[] findPinIDs(Node<VSensorConfig> startNode, Node<VSensorConfig> endNode) {
		HashSet<String> pinIDs = new HashSet<String>();
		VSensorConfig config = startNode.getObject();
		Collection<InputStream> inputStreams = config.getInputStreams();
		for (InputStream stream : inputStreams) {
			StreamSource[] sources = stream.getSources();
			for (int i = 0; i < sources.length; i++) {
				AddressBean[] addressing = sources[i].getAddressing();
				for (int j = 0; j < addressing.length; j++) {
					String vsensorName = addressing[i].getPredicateValue("NAME");
					if (vsensorName != null && vsensorName.equalsIgnoreCase(endNode.getObject().getName().trim())) {
						pinIDs.add(sources[i].getUIDStr().toString());
					}
				}
			}
		}
		return pinIDs.toArray(new String[0]);
	}

	void setVSDependencyGraph(Graph<VSensorConfig> graph, boolean checkForModification) {
		if (checkForModification == false || runningGraphhModified(graph)) {
			createSceneFromVSDependencyGraph(graph);
		}
		oldRunningGraph = graph;
	}

	/**
	 * Returns whether virtual sensor graph have been modified
	 * 
	 * @param graph
	 * @return true if any modification have been found
	 */
	public boolean onDiskGraphModified(Graph<VSensorConfig> graph) {
		// TODO : complete this method
		if (oldOnDiskGraph == null)
			return true;
		ArrayList<Node<VSensorConfig>> newNodes = graph.getNodes();
		ArrayList<Node<VSensorConfig>> oldNodes = oldOnDiskGraph.getNodes();
		for (Node<VSensorConfig> node : newNodes) {
			int index = oldNodes.indexOf(node);
			if (index != -1) {
				Node<VSensorConfig> oldNode = oldNodes.get(index);
				if (oldNode.getObject().getLastModified() != node.getObject().getLastModified())
					return true;
			} else {
				return true;
			}
		}
		for (Node<VSensorConfig> node : oldNodes) {
			if (newNodes.contains(node) == false)
				return true;
		}
		return false;
	}
	
	public boolean runningGraphhModified(Graph<VSensorConfig> graph) {
//		 TODO : complete this method
		if (oldRunningGraph == null)
			return true;
		ArrayList<Node<VSensorConfig>> newNodes = graph.getNodes();
		ArrayList<Node<VSensorConfig>> oldNodes = oldRunningGraph.getNodes();
		for (Node<VSensorConfig> node : newNodes) {
			int index = oldNodes.indexOf(node);
			if (index != -1) {
				Node<VSensorConfig> oldNode = oldNodes.get(index);
				if (oldNode.getObject().getLastModified() != node.getObject().getLastModified())
					return true;
			} else {
				return true;
			}
		}
		for (Node<VSensorConfig> node : oldNodes) {
			if (newNodes.contains(node) == false)
				return true;
		}
		return false;
	}
	
	public void setOldRunningGraph(Graph<VSensorConfig> graph){
		oldRunningGraph = graph;
	}

	public void addChangeListener(VSensorGraphListener listener) {
		if (listeners.contains(listener) == false)
			listeners.add(listener);
	}

	public void removeChangeListener(VSensorGraphListener listener) {
		listeners.remove(listener);
	}

	public void removeAllListeners() {
		listeners.clear();
	}

	public Collection<VSVNodeWidget> getAllNodeWidgets() {
		return nodeIDToNodeWidgetMap.values();
	}

	public WidgetAction getPopupAction() {
		return popupAction;
	}

	public void setPopupAction(WidgetAction popupAction) {
		this.popupAction = popupAction;
	}

	/**
	 * Hides or shows the widget and its depending widgets
	 * 
	 * @param widget
	 */
	public void changeVisibility(VSVNodeWidget widget) {
		if (oldRunningGraph == null)
			return;
		boolean visible = !widget.isVisible();
		Node<VSensorConfig> graphNode = oldRunningGraph.findNode((VSensorConfig) findObject(widget));
		if (visible == false) {
			List<Node<VSensorConfig>> nodesAffectedByRemoval = oldRunningGraph.nodesAffectedByRemoval(graphNode);
			for (Node<VSensorConfig> node : nodesAffectedByRemoval) {
				Widget widget2 = findWidget(node.getObject());
				widget2.setVisible(visible);
				for (LayoutRelationship layoutRelationship : relationShips) {
					if (layoutRelationship.getDestinationInLayout().equals(widget2)) {
						((VSVNodeWidget) layoutRelationship.getDestinationInLayout()).setVisible(visible);
						((VSVConnectionWidget) layoutRelationship).setVisible(visible);
					}
				}
			}
		} else {
			List<Node<VSensorConfig>> descendingNodes = oldRunningGraph.getDescendingNodes(graphNode);
			for (Node<VSensorConfig> node : descendingNodes) {
				Widget widget2 = findWidget(node.getObject());
				widget2.setVisible(visible);
				for (LayoutRelationship layoutRelationship : relationShips) {
					if (layoutRelationship.getDestinationInLayout().equals(widget2)) {
						((VSVNodeWidget) layoutRelationship.getDestinationInLayout()).setVisible(visible);
						((VSVConnectionWidget) layoutRelationship).setVisible(visible);
					}
				}
			}
		}

		widget.setVisible(visible);
		validate();
	}

	public LayoutAlgorithm getLayoutAlgorithm() {
		return layoutAlgorithm;
	}

	public void setLayoutAlgorithm(LayoutAlgorithm layoutAlgorithm) {
		this.layoutAlgorithm = layoutAlgorithm;
	}

	public void removeAllNodes() {
		String[] nodeIDs = getNodes().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		for (int i = 0; i < nodeIDs.length; i++) {
			removeNodeWithEdges(nodeIDs[i]);
			// notifying all listeners
			for (VSensorGraphListener listener : listeners) {
				listener.nodeRemoving((VSensorConfig) findObject(nodeIDToNodeWidgetMap.get(nodeIDs[i])), nodeIDToNodeWidgetMap.get(nodeIDs[i]));
			}
			nodeIDToNodeWidgetMap.remove(nodeIDs[i]);
		}
	}
}
