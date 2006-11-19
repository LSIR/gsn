/**
 * 
 * @author Jerome Rousselot
 */
package gsn.gui;

import gsn.gui.forms.StartStopEventListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;


/**
 * @author jerome
 * 
 */
public class GSNStatusBar extends JPanel implements StartStopEventListener {

    JLabel gsnstatus, dirstatus;

    private static final String GSN_RUNNING = "GSN running.",
	    GSN_STOPPED = "GSN stopped.",
	    DIR_RUNNING = "Directory service enabled.",
	    DIR_STOPPED = "Directory service disabled.";

    private static final Icon green = new ImageIcon("icons/running.png"),
	    red = new ImageIcon("icons/stopped.png");

    public GSNStatusBar() {
	super();

	gsnstatus = new JLabel(GSN_STOPPED);
	dirstatus = new JLabel(DIR_STOPPED);
	gsnstatus.setIcon(red);
	dirstatus.setIcon(red);
	this.add(gsnstatus);
	JSeparator separator = new JSeparator(JSeparator.VERTICAL);

	this.add(separator);
	this.add(dirstatus);
    }

    /*
         * (non-Javadoc)
         * 
         * @see gsn.gui.forms.StartStopEventListener#notifyGSNDirStart()
         */
    public void notifyGSNDirStart() {
	dirstatus.setText(DIR_RUNNING);
	dirstatus.setIcon(green);
    }

    /*
         * (non-Javadoc)
         * 
         * @see gsn.gui.forms.StartStopEventListener#notifyGSNDirStop()
         */
    public void notifyGSNDirStop() {
	dirstatus.setText(DIR_STOPPED);
	dirstatus.setIcon(red);
    }

    /*
         * (non-Javadoc)
         * 
         * @see gsn.gui.forms.StartStopEventListener#notifyGSNStart()
         */
    public void notifyGSNStart() {
	gsnstatus.setText(GSN_RUNNING);
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
