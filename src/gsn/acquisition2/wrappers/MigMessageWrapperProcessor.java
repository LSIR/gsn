package gsn.acquisition2.wrappers;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;
import gsn.acquisition2.messages.DataMsg;
import gsn.beans.DataField;

public class MigMessageWrapperProcessor extends SafeStorageAbstractWrapper {

	private MigMessageParameters parameters = null;

	private Class classTemplate = null;
	
	private Constructor messageConstructor = null;

	private final transient Logger logger = Logger.getLogger( MigMessageWrapperProcessor.class );

	public boolean initialize() {
		logger.warn("tinyos processor wrapper initialize started...");
		if (! super.initialize()) return false; 
		try {
			parameters = new MigMessageParameters () ;
			parameters.initParameters(getActiveAddressBean());			
			//
			classTemplate = Class.forName(parameters.getTinyosMessageName());
			parameters.buildOutputStructure(classTemplate);
			//
			messageConstructor = classTemplate.getConstructor(byte[].class) ;
		}
		catch (RuntimeException e) {
			logger.error(e.getMessage());
			return false;
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage());
			return false;
		} catch (NoSuchMethodException e) {
			logger.error(e.getMessage());
			return false;
		}
		logger.warn("tinyos processor wrapper initialize completed...");
		return true;
	}

	@Override
	public boolean messageToBeProcessed(DataMsg dataMessage) {
		try {
			
			byte[] rawmsg = (byte[]) dataMessage.getData()[0];
			Long ts = (Long) dataMessage.getData()[1];
			
			String val = "";
			for (int i = 0 ; i < rawmsg.length ; i++) {
				val += rawmsg[i] + " " ; 
			}
			logger.debug("new message to be processed: " + val);
			
			
			
			Object msg = (Object) messageConstructor.newInstance(rawmsg);
			
			//
			ArrayList<Serializable> output = new ArrayList<Serializable> () ;
			Iterator<Method> iter = parameters.getGetters().iterator();
			Method getter = null;
			Object res = null;
			Serializable resarray = null;
			while (iter.hasNext()) {
				getter = (Method) iter.next();
				getter.setAccessible(true);
				res = getter.invoke(msg);
				if (getter.getReturnType().isArray()) {
					for(int i = 0 ; i < Array.getLength(res) ; i++) {
						resarray = (Serializable) Array.get(res, i);
						output.add(resarray);
						logger.debug("> " + getter.getName() + ": " + resarray);
					}
				}
				else {
					output.add((Serializable)res);
					logger.debug("> " + getter.getName() + ": " + res);
				}
			}
			
			logger.debug("LENGTH: " + output.toArray().length);
			
			postStreamElement(ts.longValue(), output.toArray(new Serializable[] {}));

		} catch (InstantiationException e) {
			logger.error(e.getMessage());
		} catch (IllegalAccessException e) {
			logger.error(e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
		} catch (InvocationTargetException e) {
			logger.error(e.getMessage());
		} catch (SecurityException e) {
			logger.error(e.getMessage());
		}
		return true;
	}

	@Override
	public DataField[] getOutputFormat() {
		return parameters.getOutputStructure() ;
	}
}
