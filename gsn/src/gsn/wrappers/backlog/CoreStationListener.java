package gsn.wrappers.backlog;

import java.net.InetAddress;

public interface CoreStationListener {

	public void processData(byte[] data, int count);
	
	
	public String getCoreStationName();


    public int getPort();


    public InetAddress getInetAddress();

    
    public void connectionLost();
    
    
    public void connectionEstablished();
}
