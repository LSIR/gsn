package gsn.tests.helpers;

import gsn.utils.ChainOfReponsibility;
import gsn.core.OperatorConfig;

public class EmptyChainOfResponsiblity extends ChainOfReponsibility<OperatorConfig> {
  protected boolean handle(OperatorConfig operatorConfig) {
    return false;
  }
}
