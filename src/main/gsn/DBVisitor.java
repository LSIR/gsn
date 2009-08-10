package gsn;

import gsn2.wrappers.WrapperConfig;
import gsn.core.OperatorConfig;

public class DBVisitor implements ConfigurationVisitor{

	public void visit(WrapperConfig config) {
		System.out.println(config);
	}


	public void visit(OperatorConfig config) {
		System.out.println(config.toString());
	}

}
