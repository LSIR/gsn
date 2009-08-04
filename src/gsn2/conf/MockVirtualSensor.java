package gsn2.conf;

public class MockVirtualSensor implements VirtualSensor {
	private VirtualSensorConfig conf;

	public MockVirtualSensor(VirtualSensorConfig conf) {
		this.conf = conf;
	}
}
