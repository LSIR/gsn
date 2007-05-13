package gsn.gui.util;

import java.awt.Component;
import java.awt.Dimension;

public class GUIUtil {
	public static void locateOnOpticalScreenCenter(Component component) {
        Dimension paneSize = component.getSize();
        Dimension screenSize = component.getToolkit().getScreenSize();
        component.setLocation(
            (screenSize.width  - paneSize.width)  / 2,
            (int) ((screenSize.height - paneSize.height) *0.45));
    }
}
