package gsn.core;

import org.picocontainer.Disposable;
import gsn2.conf.OperatorConfig;

public interface VSensorStateChangeListener extends Disposable{

	public boolean vsLoading(OperatorConfig config);
	
	public boolean vsUnLoading(OperatorConfig config);
	
}
