package gsn2.conf;

import org.picocontainer.Disposable;
import org.picocontainer.Startable;

public class InputStream implements Startable,Disposable {
	public InputStream(InputStreamConfig config,VirtualSensor vs) {
		
	}

	public void start() {
		System.out.println("InputStream started");
	}

	public void stop() {
		
	}

	public void dispose() {
		
	}
}
