package gsn.wrappers;

import gsn.Main;
import java.util.HashMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class WrappersUtil {
  
  public static transient Logger logger= Logger.getLogger ( WrappersUtil.class );
  
  public static final String     DEFAULT_WRAPPER_PROPERTIES_FILE  = "conf/wrappers.properties";
  public static HashMap<String, Class<?>> loadWrappers(HashMap<String, Class<?>> wrappers, String location) throws ClassNotFoundException{
    Configuration config = null;
    try {// Trying to load the wrapper specified in the configuration file of the container. 
      config = new PropertiesConfiguration ( location );
    } catch ( ConfigurationException e ) {
      logger.error ( "The wrappers configuration file's syntax is not compatible." );
      logger.error ( new StringBuilder ( ).append ( "Check the :" ).append ( location ).append ( " file and make sure it's syntactically correct." ).toString ( ) );
      logger.error ( "Sample wrappers extention properties file is provided in GSN distribution." );
      logger.error ( e.getMessage ( ) , e );
      System.exit ( 1 );
    }  
    String wrapperNames[] = config.getStringArray ( "wrapper.name" );
    String wrapperClasses[] = config.getStringArray ( "wrapper.class" );
    for ( int i = 0 ; i < wrapperNames.length ; i++ ) {
      String name = wrapperNames[ i ];
      String className = wrapperClasses[ i ];
      if ( wrappers.get ( name ) != null ) {
        logger.error ( "The wrapper name : " + name + " is used more than once in the properties file." );
        logger.error ( new StringBuilder ( ).append ( "Please check the " ).append ( location ).append ( " file and try again." ).toString ( ) );
        System.exit ( 1 );
      }
      Class wrapperClass = Class.forName ( className );
      wrappers.put ( name , wrapperClass );
      if ( logger.isInfoEnabled ( ) ) logger.info ( new StringBuilder ( ).append ( "Wrapper [" ).append ( name ).append ( "] added successfully." ).toString ( ) );
    }
    return wrappers;
  }  
  public static HashMap<String, Class<?>> loadWrappers(HashMap<String, Class<?>> wrappers) throws ClassNotFoundException{
    return loadWrappers(wrappers,DEFAULT_WRAPPER_PROPERTIES_FILE);
  }
}
