package gsn.gui.forms;

import gsn.beans.VSensorConfig;

import org.netbeans.api.visual.widget.Widget;

public interface VSensorGraphListener {
	public void nodeAdded(VSensorConfig vsConfig, Widget widget);
	public void nodeRemoving(VSensorConfig vsConfig, Widget widget);
}
