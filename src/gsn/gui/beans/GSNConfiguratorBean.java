package gsn.gui.beans;

import gsn.beans.ContainerConfig;
import gsn.utils.ValidityTools;

import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.log4j.helpers.OptionConverter;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import com.jgoodies.binding.beans.ExtendedPropertyChangeSupport;

public final class GSNConfiguratorBean extends ContainerConfig {

    private ExtendedPropertyChangeSupport changeSupport = new ExtendedPropertyChangeSupport(
	    this);

    public void addPropertyChangeListener(PropertyChangeListener changeListener) {
	changeSupport.addPropertyChangeListener(changeListener);
    }

    public void removePropertyChangeListener(
	    PropertyChangeListener changeListener) {
	changeSupport.removePropertyChangeListener(changeListener);
    }

    private String directoryLoggingLevel;

    public static final String FIELD_NAME_directoryLoggingLevel = "directoryLoggingLevel";

    public void setDirectoryLoggingLevel(String newValue) {
	String oldValue = this.directoryLoggingLevel;
	this.directoryLoggingLevel = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_directoryLoggingLevel,
		oldValue, newValue);
    }

    public String getDirectoryLoggingLevel() {
	return this.directoryLoggingLevel;
    }

    private long maxDirectoryLogSizeInMB;

    public static final String FIELD_NAME_maxDirectoryLogSizeInMB = "maxDirectoryLogSizeInMB";

    public void setMaxDirectoryLogSizeInMB(long newValue) {
	long oldValue = this.maxDirectoryLogSizeInMB;
	this.maxDirectoryLogSizeInMB = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_maxDirectoryLogSizeInMB,
		oldValue, newValue);
    }

    public long getMaxDirectoryLogSizeInMB() {
	return this.maxDirectoryLogSizeInMB;
    }

    private String gsnLoggingLevel;

    public static final String FIELD_NAME_gsnLoggingLevel = "gsnLoggingLevel";

    public void setGsnLoggingLevel(String newValue) {
	String oldValue = this.gsnLoggingLevel;
	this.gsnLoggingLevel = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_gsnLoggingLevel, oldValue,
		newValue);
    }

    public String getGsnLoggingLevel() {
	return this.gsnLoggingLevel;
    }

    private long maxGSNLogSizeInMB;

    public static final String FIELD_NAME_maxGSNLogSizeInMB = "maxGSNLogSizeInMB";

    public void setMaxGSNLogSizeInMB(long newValue) {
	long oldValue = this.maxGSNLogSizeInMB;
	this.maxGSNLogSizeInMB = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_maxGSNLogSizeInMB,
		oldValue, newValue);
    }

    public long getMaxGSNLogSizeInMB() {
	return this.maxGSNLogSizeInMB;
    }

    public static final String FIELD_NAME_directoryPortNo = "directoryPortNo";

    public static final int DEFAULT_DIRECTORY_PORT = 1882;

    public static final String FIELD_NAME_gsnPortNo = "containerPort";

    public void setContainerPort(int newValue) {
	int oldValue = this.containerPort;
	this.containerPort = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_gsnPortNo, oldValue,
		newValue);
    }

    public void setRegistryBootstrapAddr(String newValue) {
	String oldValue = this.registryBootstrapAddr;
	this.registryBootstrapAddr = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_registryBootstrapAddr,
		oldValue, newValue);
    }

    private String directoryLogFileName;

    public static final String FIELD_NAME_directoryLogFileName = "directoryLogFileName";

    public void setDirectoryLogFileName(String newValue) {
	String oldValue = this.directoryLogFileName;
	this.directoryLogFileName = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_directoryLogFileName,
		oldValue, newValue);
    }

    public String getDirectoryLogFileName() {
	return this.directoryLogFileName;
    }

    private String gsnLogFileName;

    public static final String FIELD_NAME_gsnLogFileName = "gsnLogFileName";

    public void setGsnLogFileName(String newValue) {
	String oldValue = this.gsnLogFileName;
	this.gsnLogFileName = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_gsnLogFileName, oldValue,
		newValue);
    }

    public String getGsnLogFileName() {
	return this.gsnLogFileName;
    }

    private String dirLog4jFile;

    private String gsnLog4jFile;

    private String gsnConfigurationFileName;

    private Properties dirLog4JProperties;

    private Properties gsnLog4JProperties;

    public static final String FIELD_NAME_directoryServiceHost = "directoryServiceHost";

    public void setWebEmail(String newValue) {
	String oldValue = this.webEmail;
	this.webEmail = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_webEmail, oldValue,
		newValue);
    }

    public void setWebAuthor(String newValue) {
	String oldValue = this.webAuthor;
	this.webAuthor = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_webAuthor, oldValue,
		newValue);
    }

    public void setWebName(String newValue) {
	String oldValue = this.webName;
	this.webName = newValue;
	changeSupport
		.firePropertyChange(FIELD_NAME_webName, oldValue, newValue);
    }

    public String getWebName() {
	return this.webName;
    }

    public static final String[] LOGGING_LEVELS = { "DEBUG", "INFO", "WARN",
	    "ERROR" };

    public static final String FIELD_NAME_jdbcDriver = "jdbcDriver";

    public void setJdbcDriver(String newValue) {
	String oldValue = this.jdbcDriver;
	this.jdbcDriver = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_jdbcDriver, oldValue,
		newValue);
    }

    public void setJdbcPassword(String newValue) {
	String oldValue = this.jdbcPassword;
	this.jdbcPassword = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_jdbcPassword, oldValue,
		newValue);
    }

    public String getJdbcPassword() {
	return this.jdbcPassword;
    }

    public void setJdbcUsername(String newValue) {
	String oldValue = this.jdbcUsername;
	this.jdbcUsername = newValue;
	changeSupport.firePropertyChange(FIELD_NAME_jdbcUsername, oldValue,
		newValue);
    }

    public String getJdbcUsername() {
	return this.jdbcUsername;
    }

    public void setJdbcURL(String newValue) {
	String oldValue = this.jdbcURL;
	this.jdbcURL = newValue;
	changeSupport
		.firePropertyChange(FIELD_NAME_jdbcURL, oldValue, newValue);
    }

    public String getJdbcURL() {
	return this.jdbcURL;
    }

    public static String[] NETWORK_ADDRESSES;

    static {
	int i = 0;
	NETWORK_ADDRESSES = new String[ValidityTools.NETWORK_LOCAL_ADDRESS
		.size()];
	for (String address : ValidityTools.NETWORK_LOCAL_ADDRESS)
	    NETWORK_ADDRESSES[i++] = address + ":" + DEFAULT_DIRECTORY_PORT;
    }

    public static final String[] JDBC_SYSTEMS = { "HSqlDB in Memory",
	    "HSqlDB in File", "MySql" };

    public static final String[] JDBC_URLS = new String[] {
	    "jdbc:hsqldb:mem:.", "jdbc:hsqldb:file:/path/to/file",
	    "jdbc:mysql://host:3306/dbName" };

    public static final String[] JDBC_DRIVERS = new String[] {
	    "org.hsqldb.jdbcDriver", "org.hsqldb.jdbcDriver",
	    "com.mysql.jdbc.Driver" };

    public static final String[] JDBC_URLS_PREFIX = new String[] {
	    "jdbc:hsqldb:mem:", "jdbc:hsqldb:file:", "jdbc:mysql:" };

    public static GSNConfiguratorBean getDefaultConfiguration() {
	GSNConfiguratorBean bean = new GSNConfiguratorBean();
	bean.setContainerPort(GSNConfiguratorBean.DEFAULT_GSN_PORT);
	bean.setRegistryBootstrapAddr("localhost:1882");
	bean.setJdbcDriver(GSNConfiguratorBean.JDBC_SYSTEMS[0]);
	bean.setJdbcPassword("");
	bean.setJdbcURL("sa");
	bean.setDirectoryLogFileName("gsn-dir.log");
	bean.setDirectoryLoggingLevel(LOGGING_LEVELS[3]);
	bean.setGsnLogFileName("gsn.log");
	bean.setGsnLoggingLevel(LOGGING_LEVELS[3]);
	bean.setJdbcURL(GSNConfiguratorBean.JDBC_URLS[0]);
	bean.setMaxDirectoryLogSizeInMB(1);
	bean.setMaxGSNLogSizeInMB(10);
	bean.setWebName("NoName.");
	bean.setWebAuthor("Author not specified.");
	bean.setWebEmail("Email not specified.");
	return bean;
    }

    public static final String DEFAULT_LOGGING_LEVEL = GSNConfiguratorBean.LOGGING_LEVELS[3];

    private static String extractLoggingLevel(String property,
	    String[] setOfPossibleValues, String defaultValue) {
	String toReturn = defaultValue;
	if (property == null)
	    return toReturn;
	StringTokenizer st = new StringTokenizer(property, ",");
	if (st == null || st.countTokens() == 0)
	    return toReturn;
	String inputLogLevel = st.nextToken();
	if (inputLogLevel == null)
	    return toReturn;
	else
	    inputLogLevel = inputLogLevel.toUpperCase().trim();
	for (String level : setOfPossibleValues)
	    if (level.equals(inputLogLevel)) {
		toReturn = level;
		break;
	    }
	return toReturn;
    }

    public static GSNConfiguratorBean getConfigurationFromFile(
	    String containerConfigurationFileName, String gsnLog4jFile,
	    String dirLog4jFile) throws JiBXException, FileNotFoundException {
	IBindingFactory bfact = BindingDirectory
		.getFactory(GSNConfiguratorBean.class);
	IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
	GSNConfiguratorBean toReturn = (GSNConfiguratorBean) uctx
		.unmarshalDocument(new FileInputStream(
			containerConfigurationFileName), null);

	Properties gsnLog4j = new Properties();
	Properties dirLog4j = new Properties();
	try {
	    gsnLog4j.load(new FileInputStream(gsnLog4jFile));
	    dirLog4j.load(new FileInputStream(dirLog4jFile));
	} catch (IOException e) {
	    System.out
		    .println("Can't read the log4j files, please check the 2nd and 3rd parameters and try again.");
	    e.printStackTrace();
	    System.exit(1);
	}
	toReturn.initLog4JProperties(gsnLog4j, dirLog4j);
	toReturn.setSourceFiles(containerConfigurationFileName, gsnLog4jFile,
		dirLog4jFile);
	return toReturn;
    }

    private void initLog4JProperties(Properties gsnLog4j, Properties dirLog4j) {
	this.gsnLog4JProperties = gsnLog4j;
	this.dirLog4JProperties = dirLog4j;
	setGsnLoggingLevel(extractLoggingLevel(gsnLog4j
		.getProperty("log4j.rootLogger"),
		GSNConfiguratorBean.LOGGING_LEVELS, DEFAULT_LOGGING_LEVEL));
	setMaxGSNLogSizeInMB(OptionConverter.toFileSize(gsnLog4j
		.getProperty("log4j.appender.file.MaxFileSize"),
		GSNConfiguratorBean.DEFAULT_GSN_LOG_SIZE)
		/ (1024 * 1024));
	this.setDirectoryLoggingLevel(extractLoggingLevel(dirLog4j
		.getProperty("log4j.rootLogger"),
		GSNConfiguratorBean.LOGGING_LEVELS, DEFAULT_LOGGING_LEVEL));
	this.setMaxDirectoryLogSizeInMB(OptionConverter.toFileSize(dirLog4j
		.getProperty("log4j.appender.file.MaxFileSize"),
		GSNConfiguratorBean.DEFAULT_GSN_LOG_SIZE)
		/ (1024 * 1024));
    }

    private void setSourceFiles(String gsnConfigurationFileName,
	    String gsnLog4jFile, String dirLog4jFile) {
	this.gsnConfigurationFileName = gsnConfigurationFileName;
	this.gsnLog4jFile = gsnLog4jFile;
	this.dirLog4jFile = dirLog4jFile;
    }

    private String databaseSystem;

    public static final String FIELD_NAME_databaseSystem = "databaseSystem";

    /**
         * One Megabyte;
         */
    public static final long DEFAULT_GSN_LOG_SIZE = 1 * 1024 * 1024;

    public void setdatabaseSystem(String newValue) {
	isdatabaseSystemInitialzied = true;
	String oldValue = this.databaseSystem;
	this.databaseSystem = newValue;
	setJdbcDriver(convertToDriver(newValue));
	if (newValue == JDBC_SYSTEMS[0]) {
	    setJdbcPassword("");
	    setJdbcUsername("sa");
	    setJdbcURL(JDBC_URLS[0]);
	} else if (newValue == JDBC_SYSTEMS[1]) {
	    setJdbcPassword("");
	    setJdbcUsername("sa");
	    setJdbcURL(JDBC_URLS[1]);
	} else if (newValue == JDBC_SYSTEMS[2]) {
	    setJdbcURL(JDBC_URLS[2]);
	}
	changeSupport.firePropertyChange(FIELD_NAME_databaseSystem, oldValue,
		newValue);
    }

    private boolean isdatabaseSystemInitialzied = false;

    public String getdatabaseSystem() {
	if (isdatabaseSystemInitialzied == false) {
	    isdatabaseSystemInitialzied = true;

	    for (int i = 0; i < JDBC_URLS_PREFIX.length; i++)
		if (getJdbcURL().toLowerCase().trim().startsWith(
			JDBC_URLS_PREFIX[i])) {
		    setdatabaseSystem(JDBC_SYSTEMS[i]);
		    break;
		}
	}
	return this.databaseSystem;
    }

    public String getJdbcDriver() {
	return super.getJdbcDriver();
    }

    private String convertToDriver(String dbSys) {
	for (int i = 0; i < JDBC_SYSTEMS.length; i++)
	    if (JDBC_SYSTEMS[i].equals(dbSys))
		return JDBC_DRIVERS[i];
	return "";
    }

    public void writeConfigurations() throws FileNotFoundException, IOException {
	gsnLog4JProperties.put("log4j.rootLogger", getGsnLoggingLevel()
		+ ",file");
	dirLog4JProperties.put("log4j.rootLogger", getDirectoryLoggingLevel()
		+ ",file");

	gsnLog4JProperties.put("log4j.appender.file.MaxFileSize",
		getMaxGSNLogSizeInMB() + "MB");
	dirLog4JProperties.put("log4j.appender.file.MaxFileSize",
		getMaxDirectoryLogSizeInMB() + "MB");

	StringTemplateGroup templateGroup = new StringTemplateGroup("gsn");
	StringTemplate st = templateGroup
		.getInstanceOf("com/xoben/gsn/gui/templates/templateConf");
	st.setAttribute("name", getWebName());
	st.setAttribute("author", getWebAuthor());
	st.setAttribute("description", getWebDescription());
	st.setAttribute("email", getWebEmail());
	st.setAttribute("db_user", getJdbcUsername());
	st.setAttribute("db_password", getJdbcPassword());
	st.setAttribute("db_driver", getJdbcDriver());
	st.setAttribute("db_url", getJdbcURL());
	st.setAttribute("gsn_port", getContainerPort());
	st.setAttribute("dir_socket", getRegistryBootstrapAddr());

	gsnLog4JProperties.store(new FileOutputStream(gsnLog4jFile), "");
	dirLog4JProperties.store(new FileOutputStream(dirLog4jFile), "");
	FileWriter writer = new FileWriter(gsnConfigurationFileName);
	writer.write(st.toString());
	writer.close();

    }

    public int extractDirectoryServicePort() {
	String rawValue = getRegistryBootstrapAddr();
	if (rawValue == null || rawValue.trim().length() == 0)
	    return -1;
	return ValidityTools.getPortNumber(rawValue);
    }

    public String extractDirectoryServiceHost() {
    	return extractDirectoryServiceHost(getRegistryBootstrapAddr());
    }

    public static String extractDirectoryServiceHost(String rawValue) {
	return ValidityTools.getHostName(rawValue);
	// rawValue = rawValue.trim();
	// if (rawValue == null || rawValue.length() == 0)
	// return "";
	// StringTokenizer stringTokenizer = new StringTokenizer(rawValue, ":");
	// if (stringTokenizer.countTokens() >= 1)
	// return stringTokenizer.nextToken().trim();
	// return "";
    }

}
