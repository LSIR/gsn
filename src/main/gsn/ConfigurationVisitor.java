package gsn;

import gsn.beans.WrapperConfig;
import gsn2.conf.OperatorConfig;

public interface ConfigurationVisitor {
	void visit(WrapperConfig config) ;
	void visit(OperatorConfig config);
}
