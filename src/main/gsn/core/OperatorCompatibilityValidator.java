package gsn.core;

import gsn.utils.ChainedReponsibility;
import gsn.core.OperatorConfig;

import java.util.ArrayList;

import org.picocontainer.MutablePicoContainer;

public class OperatorCompatibilityValidator extends ChainedReponsibility<OperatorConfig> implements OpStateChangeListener {

  private ArrayList<String> existingNames = new ArrayList<String>();

  protected boolean handle(OperatorConfig operatorConfig) {
    if (existingNames.contains(operatorConfig.getIdentifier().toLowerCase()))
      return false;
    return true;
  }

  public void opLoading(MutablePicoContainer config) {
    //todo
  }

  public void opUnLoading(MutablePicoContainer config) {
    //todo
  }

  public void dispose() {

  }
}
