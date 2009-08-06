package gsn.wrappers.tinyos;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tinyos.message.Message;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.util.PrintStreamMessenger;

import org.apache.log4j.Logger;

/** MigMessageWrapper is used for reading TinyOS 2.x generated data packets.
 * The wrapper reads packet definition from a MIG-generated .java-file and
 * uses the compiled class-file to read the appropriate data.
 */

public class MigMessageWrapper implements Wrapper , net.tinyos.message.MessageListener {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;
	// regular expression clauses for finding the field names and their types from packet definition
	private static final String NAME_PATTERN = "// Accessor methods for field: (\\w+)";
	private static final String TYPE_PATTERN = "//\\s+Field type: (\\w+(\\[\\])?),";

	private static final String INITPARAM_SOURCE = "source";
	private static final String INITPARAM_PACKETNAME = "packet-name";
	private static final String INITPARAM_PATH = "path";
	private static final String INITPARAM_CLASS_PACKAGE = "class-package";

	private String source;
	private String packetName;
	private String path;
	private String classPackage;

	private int threadCounter = 0;
	private final transient Logger logger = Logger.getLogger( MigMessageWrapper.class );

	private transient DataField [ ] outputStructureCache;

	// the interface to communicate with mote
	private MoteIF moteIF;
	private static HashMap<String, String> types;
	// hashmap for storing the field name and its type
	private HashMap<String, String> fields;
	private HashMap<String, String> outputFields;
	// arraylist for storing the field names in the correct order
	private ArrayList<String> fieldsOrdered;
	private ArrayList<String> outputFieldsOrdered;

	// class and object variables used to extract data from packets using reflection
	private Class<?> packetClass;
	private Object packetObject;

	public MigMessageWrapper(WrapperConfig conf, DataChannel channel) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		this.conf = conf;
		this.dataChannel= channel;

		source =  conf.getParameters().getPredicateValueWithException(INITPARAM_SOURCE);
		packetName =  conf.getParameters().getPredicateValueWithException(INITPARAM_PACKETNAME);
		path =  conf.getParameters().getPredicateValueWithException(INITPARAM_PATH);
		classPackage =  conf.getParameters().getPredicateValueWithDefault(INITPARAM_CLASS_PACKAGE,"gsn");

		logger.debug("Connecting to " + source);
		moteIF = new MoteIF(BuildSource.makePhoenix(source, PrintStreamMessenger.err));

		types = makeTypeMap();
		logger.debug("Reading packet definition from " + path + packetName + ".java");

		packetClass = Class.forName(classPackage + "." + packetName);
		packetObject = packetClass.newInstance();
		Message msg = (Message)packetObject;
		moteIF.registerListener(msg, this);

		outputStructureCache = createOutputStructure(path + packetName + ".java");

		if(outputStructureCache == null)
			throw new RuntimeException("Mig Wrapper can't work with an Empty Structure.");

		logger.warn("TinyOS MIG wrapper identifed the following fields from the "+(path+packetName+".java"));
		for (DataField df : outputStructureCache) 
			logger.warn("\t FieldName: "+df.getName()+" ("+df.getType()+")");

	}

	/** Creates a HashMap, where key is the type in nesC and value is the type in GSN
	 */
	private static HashMap<String, String> makeTypeMap() {
		HashMap<String, String> types = new HashMap<String, String>();
		types.put("byte", "tinyint");
		types.put("short", "smallint");
		types.put("int", "integer");
		types.put("long", "bigint");
		types.put("byte[]", "tinyint");
		types.put("short[]", "smallint");
		types.put("int[]", "integer");
		types.put("long[]", "bigint");
		return types;
	}

	/** Creates the output structure required for posting data to GSN
	 * Reads definition from a file, whose name is given as a parameter and
	 * uses regular expression 
	 */
	private DataField[] createOutputStructure(String filename) {
		fields = new HashMap<String, String>();
		fieldsOrdered = new ArrayList<String>();
		outputFields = new HashMap<String, String>();
		outputFieldsOrdered = new ArrayList<String>();
		try
		{
			BufferedReader input = new BufferedReader(new FileReader(filename));
			String data;

			Pattern namePattern = Pattern.compile(NAME_PATTERN);
			Pattern typePattern = Pattern.compile(TYPE_PATTERN);
			Matcher nameMatcher;
			Matcher typeMatcher;
			String name = null;
			String type = null;

			// Name and type aren't on the same line, so we have to clear the
			// variables only after both are read successfully.
			// There is a possibility that this might cause problems if
			// at some time the regular expressions don't match the correct
			// lines.
			// The names have to be put to a separate datastructure, which
			// retains the order: if they were stored only to a hashmap, then
			// later we wouldn't know in which order they would be and the
			// values would be set to wrong sensor names.
			while((data = input.readLine()) != null) {
				nameMatcher = namePattern.matcher(data);
				typeMatcher = typePattern.matcher(data);

				while(nameMatcher.find()) {
					name = nameMatcher.group(1);
				}
				while(typeMatcher.find()) {
					type = typeMatcher.group(1);
				}
				if(name != null && type != null) {
					if(type.contains("[]")) {
						int i=0;
						int size=0;
						Method getArraySize = packetClass.getDeclaredMethod("numElements_" + name);
						try {
							size = (Integer) getArraySize.invoke(packetObject);
						} catch (InvocationTargetException e) {
							logger.error("Invocation of numElements_" + name + " failed: %s%n", e);
						}
						for(i=0; i < size; i++) {
							outputFieldsOrdered.add(name + i);
							outputFields.put(name + i, type);
							logger.debug("Adding " + name + i + " with type " + type);                            
						}
						fieldsOrdered.add(name);
						fields.put(name, type);
						name = null;
						type = null;                        
					} else {
						fieldsOrdered.add(name);
						fields.put(name, type);
						outputFieldsOrdered.add(name);
						outputFields.put(name, type);
						logger.debug("Adding " + name + " with type " + type);
						name = null;
						type = null;
					}
				}
			}
			input.close();
		} 
		catch (Exception e) {
			logger.warn("File input error", e);
			return null;
		}

		// create an array of DataTypes for returning
		ArrayList<DataField> fieldsAL = new ArrayList<DataField>();
		Iterator<String> it = outputFieldsOrdered.iterator();
		while(it.hasNext()) {
			String curName = it.next();
			String curType = outputFields.get(curName);
			fieldsAL.add(new DataField(curName.toUpperCase(), types.get(curType)));
		}
		return fieldsAL.toArray(new DataField[] {} );
	}

	/** Implements net.tinyos.message.MessageListener.messageReceived
	 * Stores the received message to a buffer so that GSN can fetch the message
	 * from there whenever it wants.
	 */
	public void messageReceived(int to, Message message) {
		logger.debug("Received message");
		try {
			Constructor<?> packetConstructor = packetClass.getDeclaredConstructor(Message.class, int.class, int.class);
			if(message == null) {
				logger.warn("Message was null!");
				return;
			}
			packetObject = packetConstructor.newInstance(message, message.baseOffset(), message.dataLength());
		} catch(NoSuchMethodException e) {
			logger.error("Cannot create an instance of packet: constructor not found", e);
			return;
		} catch(InstantiationException e) {
			logger.error("Cannot create an instance of packet", e);
			return;
		} catch(IllegalAccessException e) {
			logger.error("Cannot create an instance of packet", e);
			return;
		} catch(InvocationTargetException e) {
			logger.error("Cannot create an instance of packet", e);
			return;
		}

		// The return values are always short, int or long.
		// Here we don't have to mind about it as they are all serializable, and
		// we can return an array of serializable-objects.
		ArrayList<Serializable> retvals = new ArrayList<Serializable>();            
		Method[] allMethods = packetClass.getDeclaredMethods();

		for(String fieldName : fieldsOrdered) {
			boolean isArray = checkArray(fieldName);
			for (Method method : allMethods) {
				String methodName = method.getName();
				try {
					if(methodName.equals("get_" + fieldName)) {
						method.setAccessible(true);
						Serializable value = (Serializable) method.invoke(packetObject);
						if (logger.isDebugEnabled()) {
							logger.debug("Using " + methodName);
							logger.debug("Got: " + value.getClass().getCanonicalName());
						}
						if(isArray) {
							for(int i=0; i < Array.getLength(value); i++) {
								retvals.add((Serializable) Array.get(value, i));
							}
						} else {
							retvals.add((Serializable) method.invoke(packetObject));
						}
					}
				} catch (InvocationTargetException e) {
					logger.error("Invocation of " + methodName + " failed: %s%n", e);
				} catch (IllegalAccessException e) {
					logger.error("Cannot access " + methodName, e);
				}
			}
		}

    Serializable[] readings = retvals.toArray(new Serializable[]{});
    StreamElement streamEle = StreamElement.from(this);
                for (int i=0;i<getOutputFormat().length;i++)
                  streamEle.set(getOutputFormat()[i].getName(),readings[i]);
    streamEle.setTime(System.currentTimeMillis());

    dataChannel.write( streamEle);
	}

	private boolean checkArray(String fieldName) {
		String type = fields.get(fieldName);
		if(type.contains("[]")) return true;
		else return false;
	}

	/** Overrides gsn.wrappers.AbstractWrapper.run
	 * Nothing to do, because the values are inserted to GSN whenever packets arrive.
	 */
	public void start(){
	}

	private boolean isActive=true;

	public void dispose ( ) {
	}
	

	public void stop() {
		isActive = false;
	}

	public final DataField [ ] getOutputFormat ( ) {
		return outputStructureCache;
	}


	/**
	 * Can be used by other classes which are extending this class to create elaborated communication such
	 * as send a packet back to the sensor network.
	 * @return
	 */
	protected MoteIF getMoteIF() {
		return moteIF;
	}


}
