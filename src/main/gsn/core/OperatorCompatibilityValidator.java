package gsn.core;

import gsn.utils.ChainOfReponsibility;
import gsn.core.OperatorConfig;

import java.util.ArrayList;

import org.picocontainer.MutablePicoContainer;

public class OperatorCompatibilityValidator extends ChainOfReponsibility<OperatorConfig> implements OpStateChangeListener {

  private ArrayList<String> existingNames = new ArrayList<String>();

  protected boolean handle(OperatorConfig operatorConfig) {
    if (existingNames.contains(operatorConfig.getIdentifier().toLowerCase()))
      return false;
    return true;
  }

  public void opLoading(MutablePicoContainer config) {
    existingNames.add(config.getComponent(OperatorConfig.class).getIdentifier().toLowerCase());
  }

  public void opUnLoading(MutablePicoContainer config) {
    existingNames.remove(config.getComponent(OperatorConfig.class).getIdentifier().toLowerCase());
  }

  public void dispose() {

  }
}
