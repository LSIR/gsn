package gsn;

import gsn.beans.ContainerConfig;
import gsn.storage.StorageManager;
import gsn.utils.ValidityTools;
import gsn.wrappers.WrappersUtil;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.*;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Properties;


/**
 * Web Service URL : http://localhost:22001/services/Service?wsdl
 */
public final class Main {

    private static Main singleton;

    private Main() throws Exception {
        System.out.println("GSN Starting ...");
        ValidityTools.checkAccessibilityOfFiles(DEFAULT_GSN_LOG4J_PROPERTIES, WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE, DEFAULT_GSN_CONF_FILE);
        ValidityTools.checkAccessibilityOfDirs(DEFAULT_VIRTUAL_SENSOR_DIRECTORY);
        PropertyConfigurator.configure(Main.DEFAULT_GSN_LOG4J_PROPERTIES);
        //  initializeConfiguration();
        try {
            containerConfig = loadContainerConfiguration();
        } catch (FileNotFoundException e) {
            logger.error(new StringBuilder().append("The the configuration file : conf/gsn.xml").append(" doesn't exist.").toString());
            logger.error(e.getMessage());
            logger.error("Check the path of the configuration file and try again.");
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            return;
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
            return;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return;
        }
        StorageManager.getInstance().init(containerConfig.getJdbcDriver(), containerConfig.getJdbcUsername(), containerConfig.getJdbcPassword(), containerConfig.getJdbcURL());
        if (logger.isInfoEnabled()) logger.info("The Container Configuration file loaded successfully.");

        final VSensorLoader vsloader = new VSensorLoader(DEFAULT_VIRTUAL_SENSOR_DIRECTORY);
    }

    public synchronized static Main getInstance() {
        if (singleton == null)
            try {
                singleton = new Main();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
            }
        return singleton;
    }

    public static final String DEFAULT_GSN_LOG4J_PROPERTIES = "conf/log4j.properties";

    public static transient Logger logger = Logger.getLogger(Main.class);

    public static final String DEFAULT_GSN_CONF_FILE = "conf/gsn.xml";

    public static String DEFAULT_VIRTUAL_SENSOR_DIRECTORY = "virtual-sensors";

    public static final String DEFAULT_WEB_APP_PATH = "webapp";


    public static void main(String[] args) {
        Main.getInstance();
    }

    /**
     * Mapping between the wrapper name (used in addressing of stream source)
     * into the class implementing DataSource.
     */
    private static Properties wrappers;

    private ContainerConfig containerConfig;

    public static ContainerConfig loadContainerConfiguration() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException, SecurityException, SignatureException, IOException {
        ValidityTools.checkAccessibilityOfFiles(Main.DEFAULT_GSN_LOG4J_PROPERTIES, WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE, Main.DEFAULT_GSN_CONF_FILE);
        ValidityTools.checkAccessibilityOfDirs(Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY);
        PropertyConfigurator.configure(Main.DEFAULT_GSN_LOG4J_PROPERTIES);
        ContainerConfig toReturn = null;
        try {
            toReturn = loadContainerConfig(DEFAULT_GSN_CONF_FILE);
            wrappers = WrappersUtil.loadWrappers(new HashMap<String, Class<?>>());
            if (logger.isInfoEnabled())
                logger.info(new StringBuilder().append("Loading wrappers.properties at : ").append(WrappersUtil.DEFAULT_WRAPPER_PROPERTIES_FILE).toString());
            if (logger.isInfoEnabled()) logger.info("Wrappers initialization ...");
        } catch (JiBXException e) {
            logger.error(e.getMessage());
            logger.error(new StringBuilder().append("Can't parse the GSN configuration file : conf/gsn.xml").toString());
            logger.error("Please check the syntax of the file to be sure it is compatible with the requirements.");
            logger.error("You can find a sample configuration file from the GSN release.");
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            System.exit(1);
        } catch (FileNotFoundException e) {
            logger.error(new StringBuilder().append("The the configuration file : conf/gsn.xml").append(" doesn't exist.").toString());
            logger.error(e.getMessage());
            logger.error("Check the path of the configuration file and try again.");
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            System.exit(1);
        } catch (ClassNotFoundException e) {
            logger.error("The file wrapper.properties refers to one or more classes which don't exist in the classpath");
            logger.error(e.getMessage(), e);
            System.exit(1);
        } finally {
            return toReturn;
        }
    }

    /**
     * This method is called by Rails's Application.rb file.
     */
    public static ContainerConfig loadContainerConfig(String gsnXMLpath) throws JiBXException, FileNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, IOException, KeyStoreException, CertificateException, SecurityException, SignatureException, InvalidKeyException, ClassNotFoundException {
        if (!new File(gsnXMLpath).isFile()) {
            logger.fatal("Couldn't find the gsn.xml file @: " + (new File(gsnXMLpath).getAbsolutePath()));
            System.exit(1);
        }
        IBindingFactory bfact = BindingDirectory.getFactory(ContainerConfig.class);
        IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
        ContainerConfig conf = (ContainerConfig) uctx.unmarshalDocument(new FileInputStream(new File(gsnXMLpath)), null);
        Class.forName(conf.getJdbcDriver());
        conf.setContainerConfigurationFileName(gsnXMLpath);
        return conf;
    }

    //FIXME: COPIED_FOR_SAFE_STOAGE
    public static Properties getWrappers() {
        if (singleton == null)
            return WrappersUtil.loadWrappers(new HashMap<String, Class<?>>());
        return singleton.wrappers;
    }

    private static final String PUBLIC_KEY_FILE = ".public_key";

    private static final String PRIVATE_KEY_FILE = ".private_key";

    public static void initPKI(String publicKeyFile, String privateKeyFile) throws NoSuchAlgorithmException, NoSuchProviderException, FileNotFoundException, IOException, KeyStoreException, CertificateException, SecurityException, SignatureException, InvalidKeyException {
        // TODO  : Use the pri/pub keys if they exist. (needs verification first).
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(512, random);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        File privateF = new File(privateKeyFile);
        File publicF = new File(publicKeyFile);
        publicF.createNewFile();
        privateF.createNewFile();
        OutputStream output = new FileOutputStream(privateF);
        output.write(priv.getEncoded());
        output.close();
        output = new FileOutputStream(publicF);
        output.write(pub.getEncoded());
        output.close();
        KeyStore ksca = KeyStore.getInstance("JKS", "SUN");
        ksca.load(null, null);
        logger.warn("Public and Private keys are generated successfully.");
    }

    private static PrivateKey readPrivateKey() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileInputStream keyfis = new FileInputStream(PRIVATE_KEY_FILE);
        byte[] encKey = new byte[keyfis.available()];
        keyfis.read(encKey);
        keyfis.close();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        return keyFactory.generatePrivate(privKeySpec);
    }

    private static PublicKey readPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileInputStream keyfis = new FileInputStream(PUBLIC_KEY_FILE);
        byte[] encKey = new byte[keyfis.available()];
        keyfis.read(encKey);
        keyfis.close();
        PKCS8EncodedKeySpec pubKeySpec = new PKCS8EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        return keyFactory.generatePublic(pubKeySpec);
    }

    //FIXME: COPIED_FOR_SAFE_STOAGE
    public static Class<?> getWrapperClass(String id) {
        try {
            String className = getWrappers().getProperty(id);
            if (className == null) {
                logger.error("The requested wrapper: " + id + " doesn't exist in the wrappers.properties file.");
                return null;
            }

            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * Get's the GSN configuration without starting GSN.
     *
     * @return
     * @throws Exception
     */
    public static ContainerConfig getContainerConfig() {
        if (singleton == null)
            try {
                return loadContainerConfig(DEFAULT_GSN_CONF_FILE);
            } catch (Exception e) {
                return null;
            }
        else
            return singleton.containerConfig;
    }

    public static String randomTableNameGenerator(int length) {
        byte oneCharacter;
        StringBuffer result = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            oneCharacter = (byte) ((Math.random() * ('z' - 'a' + 1)) + 'a');
            result.append((char) oneCharacter);
        }
        return result.toString();
    }

    public static int tableNameGenerator() {
        return randomTableNameGenerator(15).hashCode();
    }

    /**
     * This method is used ONLY for ORACLE DB.
     * ADDS the postfix at the end of the tableName. If the table name ends with " then
     * updates it properly.
     *
     * @param table_name
     * @return
     */
    public static String tableNamePostFixAppender(CharSequence table_name, String postFix) {
        String tableName = table_name.toString();
        if (tableName.endsWith("\""))
            return (tableName.substring(0, tableName.length() - 2)) + postFix + "\"";
        else
            return tableName + postFix;
    }

    public static StringBuilder tableNameGeneratorInString(CharSequence tableName) {
        if (tableName.charAt(0) == '_' && StorageManager.isOracle())
            return new StringBuilder("\"").append(tableName).append("\"");
        return new StringBuilder(tableName);
    }

    public static StringBuilder tableNameGeneratorInString(int code) {
        StringBuilder sb = new StringBuilder("_");
        if (code < 0)
            sb.append("_");
        sb.append(Math.abs(code));
        return tableNameGeneratorInString(sb);
	}
}
