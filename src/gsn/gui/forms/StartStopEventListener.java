/**
 * 
 * @author Jerome Rousselot
 */
package gsn.gui.forms;

/**
 * @author jerome
 * 
 */
public interface StartStopEventListener {

    public void notifyGSNStart();

    public void notifyGSNStop();

    public void notifyGSNDirStart();

    public void notifyGSNDirStop();

}
