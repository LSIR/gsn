package gsn.beans;

import java.util.List;
import org.picocontainer.Startable;
import org.picocontainer.Disposable;


public interface Operator extends Startable, Disposable {

	public abstract void process(String name, List<StreamElement> window);

  public abstract DataField[] getStructure();

}