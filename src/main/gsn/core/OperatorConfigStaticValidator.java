package gsn.core;

import gsn.utils.ChainOfReponsibility;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;
import gsn2.conf.ChannelConfig;

import java.util.HashMap;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;

public class OperatorConfigStaticValidator extends ChainOfReponsibility<OperatorConfig>{
  
  private static transient Logger logger                              = Logger.getLogger ( OperatorConfigStaticValidator.class );

  protected boolean handle(OperatorConfig operatorConfig) {
    String name = operatorConfig.getIdentifier();

    if (name.trim().length()==0){
      logger.error("The operator name can't be empty.");
      return false;
    }
    String className = null;
    try {
      className=operatorConfig.getClassName();
      Class.forName(className);
      for (ChannelConfig cConfig : operatorConfig.getChannels())
          Class.forName(className=cConfig.getSourceConfig().getClassName());
    } catch (Exception e) {
      logger.error("Problem in loading the class: "+className,e);
      return false;
    }

    if (operatorConfig.getChannels()!=null){
      ArrayList<String> names=  new ArrayList<String>();
      for(ChannelConfig c:operatorConfig.getChannels()){
        String cName = c.getName().toLowerCase().trim();
        if (names.contains(cName)){
          logger.error("Channel names must be unique for each operator, channel-name: "+cName);
          return false;
        }
        names.add(cName);
      }
    }
    return true;
  }
}
