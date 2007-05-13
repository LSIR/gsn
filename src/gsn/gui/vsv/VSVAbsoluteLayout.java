package gsn.gui.vsv;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;

import org.eclipse.mylar.zest.layout.LayoutEntity;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.Widget;


public class VSVAbsoluteLayout implements Layout {

	public void justify(Widget widget) {
		
	}

	public void layout(Widget widget) {
		//TODo : Correct and complete this
		VSVGraphScene graphScene = (VSVGraphScene)widget;
		Collection<String> nodeIDs = graphScene.getNodes();
		for (String nodeID : nodeIDs) {
			VSVNodeWidget child = (VSVNodeWidget) graphScene.findWidget(nodeID);
			if(child.isVisible())
				child.setPreferredLocation(new Point((int)child.getXInLayout(), (int)child.getYInLayout()));
			else
				child.resolveBounds(null, new Rectangle(0, 0));
			child.revalidate();
		}
	}

	public boolean requiresJustification(Widget widget) {
		return false;
	}

}
