package gsn;

import gsn2.wrappers.WrapperConfig;
import gsn.core.OperatorConfig;

public interface ConfigurationVisitor {
	void visit(WrapperConfig config) ;
	void visit(OperatorConfig config);
}
