package gsn.beans;

import java.util.List;

import org.picocontainer.Disposable;
import org.picocontainer.Startable;

public interface Operator extends Disposable,Startable {

	public abstract void process(String name, List<StreamElement> window);

  public abstract DataField[] getStructure();

}