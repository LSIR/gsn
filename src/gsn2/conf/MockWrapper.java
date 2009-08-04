package gsn2.conf;

public class MockWrapper implements Wrapper {
	
	public MockWrapper(WrapperConfig config, DataChannel channel) {
		
	}

	public void start() {
		System.out.println("Wrapper Started");
	}

	public void stop() {
		System.out.println("Wrapper Stopped");
	}

	public void dispose() {
		System.out.println("Wrapper Disposed");
	}

}
