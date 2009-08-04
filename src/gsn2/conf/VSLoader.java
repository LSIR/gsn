package gsn2.conf;

import static org.picocontainer.behaviors.Behaviors.caching;
import static org.picocontainer.behaviors.Behaviors.synchronizing;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.ComponentFactory;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.PicoBuilder;
import org.picocontainer.PicoContainer;
import org.picocontainer.visitors.AbstractPicoVisitor;

public class VSLoader {
	public static void main(String[] args) {

		//		VirtualSensor sampleVS = createMock(VirtualSensor.class);
		MutablePicoContainer picoVS = new PicoBuilder().withBehaviors(synchronizing(), caching()).withLifecycle().build();

		MutablePicoContainer picoInputStream = new PicoBuilder(picoVS).withBehaviors(synchronizing(), caching()).withLifecycle().build();

		picoVS.addChildContainer(picoInputStream);
		MutablePicoContainer picoSource = new PicoBuilder(picoInputStream).withBehaviors(synchronizing(), caching()).withLifecycle().build();
		picoInputStream.addChildContainer(picoSource);
		MutablePicoContainer picoWrapper= new PicoBuilder(picoSource).withBehaviors(synchronizing(), caching()).withLifecycle().build();
		picoSource.addChildContainer(picoWrapper);

		picoVS.addComponent(VirtualSensor.class, MockVirtualSensor.class);
		picoVS.addComponent(new VirtualSensorConfig());
		picoVS.setName("MyVsName");

		picoInputStream.addComponent(InputStream.class);
		picoInputStream.addComponent(InputStreamConfig.class,new InputStreamConfig());
		picoInputStream.setName("InputStreamName");

		picoSource.addComponent(StreamSource.class);
		picoSource.addComponent(StreamSourceConfig.class,new StreamSourceConfig());
		picoSource.setName("StreamSourceName");

		picoWrapper.addComponent(Wrapper.class,MockWrapper.class);
		picoWrapper.addComponent(new WrapperConfig());
		picoWrapper.addComponent(DataChannel.class,SourceDataChannel.class);
		picoWrapper.setName("WrapperName");

		picoWrapper.getComponent(Wrapper.class);

		picoVS.start();

		picoVS.accept(new AbstractPicoVisitor() {

			public void visitComponentAdapter(ComponentAdapter<?> arg0) {
				
//				if (arg0.getComponentImplementation().getClass() instanceof Wrapper)
//					System.out.println(1);
			}

			public void visitComponentFactory(ComponentFactory arg0) {
				
			}

			public boolean visitContainer(PicoContainer container) {
				return true;
			}

			public void visitParameter(Parameter arg0) {
				
			}});
		picoVS.dispose();


	}
}
