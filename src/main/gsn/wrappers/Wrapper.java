package gsn.wrappers;

import gsn.beans.DataField;

import org.picocontainer.Disposable;
import org.picocontainer.Startable;

public interface Wrapper extends Startable,Disposable{
	public DataField[] getOutputFormat();
}
