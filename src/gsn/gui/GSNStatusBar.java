package gsn.gui;

import gsn.gui.forms.GSNConfiguratorPanel;
import gsn.gui.forms.StartStopEventListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class GSNStatusBar extends JPanel implements StartStopEventListener {

    JLabel gsnstatus, dirstatus;

    private static  String GSN_RUNNING = "GSN running.",
	    GSN_STOPPED = "GSN stopped.";

    private static final Icon green = new ImageIcon("icons/running.png"),
	    red = new ImageIcon("icons/stopped.png");

    public GSNStatusBar() {
    	super();

    	gsnstatus = new JLabel(GSN_STOPPED);
    	gsnstatus.setIcon(red);
    	this.add(gsnstatus);
    	JSeparator separator = new JSeparator(JSeparator.VERTICAL);
    }


    public void notifyGSNStart() {
	gsnstatus.setText(GSN_RUNNING+"(http://localhost:"+GSNConfiguratorPanel.getInstance( ).getGsnPortNo( )+")");
	gsnstatus.setIcon(green);
    }

    /*
         * (non-Javadoc)
         * 
         * @see gsn.gui.forms.StartStopEventListener#notifyGSNStop()
         */
    public void notifyGSNStop() {
	gsnstatus.setText(GSN_STOPPED);
	gsnstatus.setIcon(red);
    }

}
