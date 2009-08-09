package gsn;

import gsn.beans.WrapperConfig;
import gsn2.conf.OperatorConfig;

public class DBVisitor implements ConfigurationVisitor{

	public void visit(WrapperConfig config) {
		System.out.println(config);
	}


	public void visit(OperatorConfig config) {
		System.out.println(config.toString());
	}

}
