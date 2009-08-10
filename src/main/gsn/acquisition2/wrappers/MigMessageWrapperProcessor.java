package gsn.acquisition2.wrappers;

import gsn.beans.DataField;
import gsn2.wrappers.WrapperConfig;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class MigMessageWrapperProcessor{

	private MigMessageParameters parameters = null;

	private Class<?> classTemplate = null;

	private Constructor<?> messageConstructor = null;

	private final transient Logger logger = Logger.getLogger( MigMessageWrapperProcessor.class );

	public MigMessageWrapperProcessor(WrapperConfig config) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
		logger.warn("tinyos processor wrapper initialize started...");
		parameters = new MigMessageParameters () ;
		parameters.initParameters(config);			
		//
		classTemplate = Class.forName(parameters.getTinyosMessageName());
		parameters.buildOutputStructure(classTemplate, new ArrayList<DataField>(), new ArrayList<Method>());
		//
		messageConstructor = classTemplate.getConstructor(byte[].class) ;			


	}

	public boolean messageToBeProcessed() {
//		public boolean messageToBeProcessed(DataMsg dataMessage) {

		Method getter = null;
		Object res = null;
		Serializable resarray = null;
		byte[] rawmsg = null; //(byte[]) dataMessage.getData()[0];

		try {

			Object msg = (Object) messageConstructor.newInstance(rawmsg);

			ArrayList<Serializable> output = new ArrayList<Serializable> () ;
			Iterator<Method> iter = parameters.getGetters().iterator();
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

			// Update TIMED field
			if (parameters.getTimedFieldGetter() != null) {
				logger.debug("Update TIMED field");
				parameters.getTimedFieldGetter().setAccessible(true);
				Long ts = (Long) parameters.getTimedFieldGetter().invoke(msg);
//				dataChannel.write(ts.longValue(), output.toArray(new Serializable[] {}));
			}
			else {
//				dataChannel.write(output.toArray(new Serializable[] {}));
			}
		} catch (InstantiationException e) {
			logger.error("Unable to instanciate the message");
		} catch (IllegalAccessException e) {
			logger.error("Illegal Access to >" + getter + "<");
		} catch (IllegalArgumentException e) {
			logger.error("Illegal argument to >" + getter + "<");
		} catch (InvocationTargetException e) {
			logger.error("Invocation Target Exception " + e.getMessage());
		} catch (SecurityException e) {
			logger.error("Security Exception " + e.getMessage());
		}
		return true;
	}

	public DataField[] getOutputFormat() {
		return parameters.getOutputStructure() ;
	}

	public boolean isTimeStampUnique() {
		return false;
	}
}
