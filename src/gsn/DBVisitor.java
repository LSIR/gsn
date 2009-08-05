package gsn;

import gsn.beans.WrapperConfig;
import gsn2.conf.OperatorConfig;
import gsn2.conf.SQLOperatorConfig;
import gsn2.conf.StreamConfig;

public class DBVisitor implements ConfigurationVisitor{

	public void visit(WrapperConfig config) {
		System.out.println(config);
	}

	public void visit(SQLOperatorConfig config) {
		System.out.println(config);
	}

	public void visit(StreamConfig config) {
		System.out.println(config+",,");
	}

	public void visit(OperatorConfig config) {
		System.out.println(config.toString());
	}

}
