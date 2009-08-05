package gsn;

import gsn.beans.VSFile;

public interface VSensorStateChangeListener {
	public boolean vsLoading(VSFile config);
	
	public boolean vsUnLoading(VSFile config);
	
	public void release() throws Exception;
}
