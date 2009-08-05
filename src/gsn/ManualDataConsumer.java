package gsn;

import java.io.Serializable;

public interface ManualDataConsumer {
	public boolean handleExternalInput ( String action,String[] paramNames, Serializable[] paramValues ) ;
}
