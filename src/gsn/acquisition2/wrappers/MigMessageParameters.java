package gsn.acquisition2.wrappers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import gsn.beans.AddressBean;
import gsn.beans.DataField;

public class MigMessageParameters {

	private static Hashtable<Class,String> typesMapping = null;

	private final transient Logger logger = Logger.getLogger( MigMessageParameters.class );

	private ArrayList<Method> getters = null;

	private DataField[] outputStructure = null;
	
	private Method timedFieldGetter = null;

	// Optional Parameters

	private static final String TINYOS_GETTER_PREFIX = "getter-prefix";
	private static final String TINYOS_GETTER_PREFIX_DEFAULT = "get_";
	private String tinyosGetterPrefix = null;

	private static final String TINYOS_MESSAGE_LENGTH = "message-length";
	private int tinyOSMessageLength;

	// Mandatory Parameters

	private static final String TINYOS_SOURCE = "source";
	private String tinyosSource = null;

	private static final String TINYOS_MESSAGE_NAME = "message-classname";
	private String tinyosMessageName = null;

	//private static final String TINYOS_VERSION = "tinyos-version";
	public static final byte TINYOS_VERSION_1 = 0x01;
	public static final byte TINYOS_VERSION_2 = 0x02;
	private byte tinyosVersion = 0;

	public void initParameters (AddressBean infos) {

		// Mandatory parameters (may thow RuntimeException)

		tinyosSource = infos.getPredicateValueWithException(TINYOS_SOURCE) ;

		tinyosMessageName = infos.getPredicateValueWithException(TINYOS_MESSAGE_NAME) ;

		// Define TinyOS version from the superclasses

		try {
			Class messageClass = Class.forName(tinyosMessageName);
			findTinyOSVersionFromClassHierarchy(messageClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to find the >" + tinyosMessageName + "< class.");
		}

		// Optional parameters

		tinyosGetterPrefix = infos.getPredicateValueWithDefault(TINYOS_GETTER_PREFIX, TINYOS_GETTER_PREFIX_DEFAULT);

		tinyOSMessageLength = Integer.parseInt(infos.getPredicateValueWithDefault(TINYOS_MESSAGE_LENGTH, "-1")) ;

	}

	public void buildOutputStructure (Class tosmsgClass) {
		logger.debug("Building output structure for class: " + tosmsgClass.getCanonicalName() + " and prefix: " + tinyosGetterPrefix);

		if (typesMapping == null) buildMappings() ;

		getters = new ArrayList<Method> () ;

		ArrayList<DataField> fields = new ArrayList<DataField> () ;
		DataField nextField = null;

		Method[] methods = tosmsgClass.getDeclaredMethods();
		Method method = null;
		//Class returnType = null;
		String type = null;
		for (int i = 0 ; i < methods.length ; i++) {

			method = methods[i];

			// select getters
			if (method.getName().startsWith(tinyosGetterPrefix)) {
				if (method.getName().toUpperCase().compareTo((tinyosGetterPrefix + "TIMED").toUpperCase()) == 0) {
					logger.warn("next data field is the TIMED field");
					timedFieldGetter = method;
				}
				else {
					type = typesMapping.get(method.getReturnType()) ;
					if (type == null) {
						logger.warn("Not managed type: >" + method.getReturnType() + "< for getter >" + method.getName() + "<. This getter is skipped.");
					}
					else {
						nextField = new DataField (method.getName().substring(tinyosGetterPrefix.length()).toUpperCase() , type) ;
						logger.debug("next data field: " + nextField);
						fields.add(nextField);
						getters.add(method);
					}
				}
			}
		}
		DataField[] fieldsArray = new DataField[fields.size()];
		
		// Sort the outputStructure and the methods

		this.outputStructure = fields.toArray(fieldsArray);
	}

	private static void buildMappings () {
		typesMapping = new Hashtable<Class, String> () ;
		typesMapping.put(byte.class, "TINYINT") ;
		typesMapping.put(short.class, "SMALLINT") ;
		typesMapping.put(int.class, "INTEGER") ;
		typesMapping.put(long.class, "BIGINT") ;
		typesMapping.put(float.class, "DOUBLE");
		typesMapping.put(double.class, "DOUBLE");
		typesMapping.put(byte[].class, "TINYINT") ;
		typesMapping.put(short[].class, "SMALLINT") ;
		typesMapping.put(int[].class, "INTEGER") ;
		typesMapping.put(long[].class, "BIGINT") ;
		typesMapping.put(float[].class, "DOUBLE");
		typesMapping.put(double[].class, "DOUBLE");
	}

	private void findTinyOSVersionFromClassHierarchy (Class messageClass) {
		Class currentMessageClass = messageClass;
		Class messageSuperClass;
		boolean found = false;
		while ( ! found ) {
			messageSuperClass = currentMessageClass.getSuperclass();
			logger.debug("message super class: " + messageSuperClass.getCanonicalName()) ;
			if (messageSuperClass == Object.class) break;
			else if (messageSuperClass == net.tinyos1x.message.Message.class) {
				logger.debug("> TinyOS v1.x message") ;
				tinyosVersion = TINYOS_VERSION_1 ;
				found = true;
			}
			else if (messageSuperClass == net.tinyos.message.Message.class) {
				tinyosVersion = TINYOS_VERSION_2 ; 
				logger.debug("> TinyOS v2.x message") ;
				found = true;
			}
			currentMessageClass = messageSuperClass;
		}
		if (! found) throw new RuntimeException ("Neither TinyOS1x (net.tinyos1x.message.Message) nor TinyOS2x (net.tinyos.message.Message) message class where found in the >" + tinyosMessageName + "< class hierarchy") ;
	}

	public String getTinyosSource() {
		return tinyosSource;
	}

	public String getTinyosMessageName() {
		return tinyosMessageName;
	}

	public ArrayList<Method> getGetters() {
		return getters;
	}

	public DataField[] getOutputStructure() {
		return outputStructure;
	}

	public String getTinyosGetterPrefix() {
		return tinyosGetterPrefix;
	}

	public byte getTinyosVersion() {
		return tinyosVersion;
	}

	public int getTinyOSMessageLength() {
		return tinyOSMessageLength;
	}

	public Method getTimedFieldGetter() {
		return timedFieldGetter;
	}
}
