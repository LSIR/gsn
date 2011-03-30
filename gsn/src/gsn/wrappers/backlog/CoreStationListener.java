package gsn.wrappers.backlog;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface CoreStationListener {

	public void processData(byte[] data, int count);
	
	
	public String getCoreStationName();


    public int getPort();


    public InetAddress getInetAddress() throws UnknownHostException;

    
    public void connectionLost();
    
    
    public void connectionEstablished();
}
