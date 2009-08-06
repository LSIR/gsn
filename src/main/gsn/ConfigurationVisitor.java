package gsn;

import gsn.beans.WrapperConfig;
import gsn2.conf.OperatorConfig;
import gsn2.conf.SQLOperatorConfig;
import gsn2.conf.StreamConfig;

public interface ConfigurationVisitor {
	void visit(WrapperConfig config) ;
	void visit(SQLOperatorConfig config) ;
	void visit(StreamConfig config) ;
	void visit(OperatorConfig config);
}
