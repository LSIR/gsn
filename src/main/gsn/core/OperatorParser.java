package gsn.core;

import java.util.HashMap;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import gsn.core.OperatorConfig;
import gsn.utils.ChainOfReponsibility;

public class OperatorParser implements FilePresenceListener{

  private static transient Logger logger                              = Logger.getLogger ( OperatorParser.class );

  private HashMap<String, OperatorConfig> knownConfigs = new HashMap<String, OperatorConfig>();

  private IUnmarshallingContext uctx;

  private ChainOfReponsibility<OperatorConfig> additionChain;
  private ChainOfReponsibility<OperatorConfig> removalChain;

  public OperatorParser(ChainOfReponsibility<OperatorConfig> additionChain,ChainOfReponsibility<OperatorConfig> removalChain) throws JiBXException {
    uctx = BindingDirectory.getFactory ( OperatorConfig.class).createUnmarshallingContext ( );
    this.additionChain = additionChain;
    this.removalChain = removalChain;
  }

  public void fileRemoval(String filePath) {
    if (!knownConfigs.containsKey(filePath))
      return;
    if (removalChain.proccess(knownConfigs.get(filePath)))
      knownConfigs.remove(filePath);
  }

  public void fileAddition(String filePath) {
    OperatorConfig config = createOperatorConfig(filePath);
    if (config==null)
      return;
    if(additionChain.proccess(config))
      knownConfigs.put(filePath, config);

  }

  public void fileChanged(String filePath) {
    OperatorConfig currentConfig = knownConfigs.get(filePath);
    OperatorConfig newConfig = createOperatorConfig(filePath);
    if (currentConfig == null )
      return;

    if (currentConfig.equals(newConfig))
      return;
    fileRemoval(filePath);
    fileAddition(filePath);
  }

  public OperatorConfig createOperatorConfig(String file)  {
    FileInputStream inputStream = null;
    OperatorConfig conf = null;
    try {
      inputStream = new FileInputStream(new File(file));
      conf = ( OperatorConfig ) uctx.unmarshalDocument (inputStream, null );
    } catch (FileNotFoundException e) {
      logger.error(e.getMessage(),e);
    } catch (JiBXException e) {
      logger.error(e.getMessage(),e);
    } finally {
      if (inputStream!=null)
        try {
          inputStream.close();
        } catch (IOException e) {
          logger.error(e.getMessage(),e);
        }
    }
    return conf;
  }
}
