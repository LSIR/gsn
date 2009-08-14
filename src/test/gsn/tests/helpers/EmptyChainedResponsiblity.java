package gsn.tests.helpers;

import gsn.utils.ChainedReponsibility;
import gsn.core.OperatorConfig;

public class EmptyChainedResponsiblity extends ChainedReponsibility<OperatorConfig> {
  protected boolean handle(OperatorConfig operatorConfig) {
    return false;
  }
}
