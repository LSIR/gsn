package gsn.utils;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * 
 * @author ali
 *
 */
public class Helpers {
public static void initLoggerToDebug(){
	Properties properties = new Properties( );
    properties.put( "log4j.rootLogger" , "DEBUG,console" );
    properties.put( "log4j.appender.console" , "org.apache.log4j.ConsoleAppender" );
    properties.put( "log4j.appender.console.Threshold" , "DEBUG" );
    properties.put( "log4j.appender.console.layout" , "org.apache.log4j.PatternLayout" );
    properties.put( "log4j.appender.console.layout.ConversionPattern" , "%-6p[%d] [%t] (%13F:%L) %3x - %m%n" );
    PropertyConfigurator.configure( properties );
   
}
}
