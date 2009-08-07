package gsn.core;

import gsn.utils.ChainOfReponsibility;
import gsn2.conf.OperatorConfig;

import java.util.ArrayList;

public class OperatorCompatibilityValidator extends ChainOfReponsibility<OperatorConfig> implements VSensorStateChangeListener{

  private ArrayList<String> existingNames = new ArrayList<String>();

  protected boolean handle(OperatorConfig operatorConfig) {
    if (existingNames.contains(operatorConfig.getIdentifier().toLowerCase()))
      return false;
    return true;
  }

  public boolean vsLoading(OperatorConfig config) {
    existingNames.add(config.getIdentifier().toLowerCase());
    return true;
  }

  public boolean vsUnLoading(OperatorConfig config) {
    existingNames.remove(config.getIdentifier().toLowerCase());
    return true;
  }

  public void dispose() {

  }
}
