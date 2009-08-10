package gsn.core;

import org.picocontainer.Disposable;
import org.picocontainer.MutablePicoContainer;

public interface OpStateChangeListener extends Disposable{

	public void opLoading(MutablePicoContainer config);
	
	public void opUnLoading(MutablePicoContainer config);

}
