package gsn.wrappers.backlog;

import java.net.InetAddress;

public interface DeploymentListener {

	public void processData(byte[] data, int count);
	
	
	public String getDeploymentName();


    public int getPort();


    public InetAddress getHostAddress();

    
    public void connectionLost();
    
    
    public void connectionEstablished();
}
