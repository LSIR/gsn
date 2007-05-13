package gsn.gui.vsv;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;

import org.netbeans.api.visual.graph.layout.GraphLayout;
import org.netbeans.api.visual.graph.layout.UniversalGraph;

/**
 * @author Mehdi Riahi
 *
 */
public class VSVGraphLayout extends GraphLayout<String, String> {

	public VSVGraphLayout(boolean animated) {
		setAnimated(animated);
	}

	@Override
	protected void performGraphLayout(UniversalGraph<String, String> graph) {
		performNodesLayout(graph, graph.getNodes());
	}

	@Override
	protected void performNodesLayout(UniversalGraph<String, String> graph, Collection<String> nodes) {
		VSVGraphScene graphScene = (VSVGraphScene) graph.getScene();
		for (String nodeID : nodes) {
			VSVNodeWidget child = (VSVNodeWidget) graphScene.findWidget(nodeID);
			if (child.isVisible()) {
				Point location = new Point((int) child.getXInLayout(), (int) child.getYInLayout());
//				graphScene.getSceneAnimator().animatePreferredLocation(child, location);
				setResolvedNodeLocation(graph, nodeID, location);
//				child.setPreferredLocation(location);
			} else
				child.resolveBounds(null, new Rectangle(0, 0));
			child.revalidate();
		}
	}

}
