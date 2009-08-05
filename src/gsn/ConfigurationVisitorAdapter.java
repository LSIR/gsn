package gsn;

import org.picocontainer.PicoContainer;
import org.picocontainer.visitors.TraversalCheckingVisitor;

public class ConfigurationVisitorAdapter extends TraversalCheckingVisitor{

	private ConfigurationVisitor delegate;

	public ConfigurationVisitorAdapter(ConfigurationVisitor delegate) {
		this.delegate = delegate;
	}

	public boolean visitContainer(PicoContainer arg0) {
		for (Object o : arg0.getComponents()) {
			if (o instanceof Visitable)
				((Visitable) o).accept(delegate);
		}
		return true;
	}

}
