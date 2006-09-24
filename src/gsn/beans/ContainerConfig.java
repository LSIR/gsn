package gsn.beans;

import gsn.utils.ValidityTools;

import java.io.File;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class ContainerConfig {

	private static final transient Logger logger = Logger
			.getLogger(ContainerConfig.class);

	public static final String NOT_PROVIDED = "Not Provided";

	public static final int DEFAULT_GSN_PORT = 22001;

	public static final int DEFAULT_STORAGE_POOL_SIZE = 100;

	public static final String DEFAULT_WRAPPER_PROPERTIES_FILE = "wrappers.properties";

	public static final String DEFAULT_VIRTUAL_SENSOR_DIRECTORY = "virtual-sensors";

	public static final String DEFAULT_WEB_APP_PATH = "webapp";

	protected String webName;

	public static final String FIELD_NAME_webName = "webName";

	protected String webAuthor;

	public static final String FIELD_NAME_webAuthor = "webAuthor";

	protected String webDescription;

	public static final String FIELD_NAME_webDescription = "webDescription";

	protected String webEmail;

	public static final String FIELD_NAME_webEmail = "webEmail";

	protected String mailServer;

	protected String smsServer;

	protected String smsPassword;

	protected int containerPort = DEFAULT_GSN_PORT;

	protected String virtualSensorsDir;

	protected String registryBootstrapAddr;

	public static final String FIELD_NAME_registryBootstrapAddr = "registryBootstrapAddr";

	protected String containerFileName;

	protected boolean jdbcOverwriteTables = Boolean.FALSE;

	public static final String FIELD_NAME_jdbcOverwriteTables = "jdbcOverwriteTables";

	protected String jdbcDriver;

	protected String jdbcUsername;

	public static final String FIELD_NAME_jdbcUsername = "jdbcUsername";

	protected String jdbcPassword;

	public static final String FIELD_NAME_jdbcPassword = "jdbcPassword";

	protected String jdbcURL;

	public static final String FIELD_NAME_jdbcURL = "jdbcURL";

	protected int storagePoolSize = DEFAULT_STORAGE_POOL_SIZE;

	protected String webAppPath;

	protected String wrapperExtentionsPropertiesFile;

	protected String notificationExtentionsPropertiesFile;

	protected boolean permanentStorage = false;

	public static final String FIELD_NAME_permanentStorageEnabled = "permanentStorage";

	public boolean isPermanentStorage() {
		return permanentStorage;
	}

	public boolean isJdbcOverwriteTables() {
		return jdbcOverwriteTables;
	}

	public String getContainerFileName() {
		return containerFileName;
	}

	public void setContainerConfigurationFileName(String containerFileName) {
		this.containerFileName = containerFileName;
	}

	public String getJdbcURL() {
		return jdbcURL;
	}

	/**
	 * @return Returns the author.
	 */
	public String getWebAuthor() {
		if (this.webAuthor == null || this.webAuthor.trim().equals(""))
			this.webAuthor = NOT_PROVIDED;
		else
			this.webAuthor = this.webAuthor.trim();
		return this.webAuthor;

	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public String getJdbcUsername() {
		return jdbcUsername;
	}

	public String getJdbcPassword() {
		return jdbcPassword;
	}

	/**
	 * @return Returns the containerPort.
	 */
	public int getContainerPort() {
		return containerPort;
	}

	/**
	 * @return Returns the webDescription.
	 */
	public String getWebDescription() {
		if (this.webDescription == null
				|| this.webDescription.trim().equals(""))
			this.webDescription = NOT_PROVIDED;
		return webDescription.trim();
	}

	/**
	 * @return Returns the webEmail.
	 */
	public String getWebEmail() {
		if (this.webEmail == null)
			this.webEmail = NOT_PROVIDED;
		return webEmail;
	}

	/**
	 * @return Returns the name.
	 */
	public String getWebName() {
		if (webName == null || this.webName.trim().equals(""))
			webName = NOT_PROVIDED;
		webName = webName.trim();
		return webName;
	}

	/**
	 * @return Returns the notificationExtentionsPropertiesFile.
	 */
	public String getNotificationExtentionsPropertiesFile() {
		return notificationExtentionsPropertiesFile.trim();
	}

	/**
	 * @return Returns the pluginsDir.
	 */
	public String getVirtualSensorsDir() {
		if (this.virtualSensorsDir == null
				|| this.virtualSensorsDir.trim().length() == 0) {
			this.virtualSensorsDir = DEFAULT_VIRTUAL_SENSOR_DIRECTORY;
		} else
			this.virtualSensorsDir = virtualSensorsDir.trim();
		return this.virtualSensorsDir;

	}

	/**
	 * @return Returns the registryBootstrapAddr.
	 */
	private boolean isRegistryBootStrapAddrInitialized = false;
/**
 * Returns null if the Registery bootstrap is not valid (e.g., null, empty, ...)
 * @return
 */
	public String getRegistryBootstrapAddr() {
		if (!isRegistryBootStrapAddrInitialized) {
			if (this.registryBootstrapAddr != null)
				this.registryBootstrapAddr = this.registryBootstrapAddr.trim();
			isRegistryBootStrapAddrInitialized=true;
		}
		return this.registryBootstrapAddr;
	}

	/**
	 * @param registryBootstrapAddr the registryBootstrapAddr to set
	 */
	public void setRegistryBootstrapAddr(String registryBootstrapAddr) {
		this.registryBootstrapAddr = registryBootstrapAddr;
	}

	/**
	 * @return Returns the storagePoolSize.
	 */
	public int getStoragePoolSize() {
		if (storagePoolSize <= 0)
			storagePoolSize = DEFAULT_STORAGE_POOL_SIZE;
		return storagePoolSize;
	}

	/**
	 * @return Returns the webAppPath.
	 */
	public String getWebAppPath() {
		if (this.webAppPath == null)
			this.webAppPath = DEFAULT_WEB_APP_PATH;
		else
			this.webAppPath = this.webAppPath.trim();
		return this.webAppPath;
	}

	/**
	 * @return Returns the wrapperExtentionsPropertiesFile.
	 */
	public String getWrapperExtentionsPropertiesFile() {
		if (this.wrapperExtentionsPropertiesFile == null
				|| this.wrapperExtentionsPropertiesFile.trim().length() == 0)
			this.wrapperExtentionsPropertiesFile = DEFAULT_WRAPPER_PROPERTIES_FILE;
		else
			this.wrapperExtentionsPropertiesFile = this.wrapperExtentionsPropertiesFile
					.trim();
		return this.wrapperExtentionsPropertiesFile;
	}

	public boolean isValied() {
		File file = new File(getVirtualSensorsDir());
		if (!file.exists() || !file.isDirectory()) {
			logger.error(" The path in the <virtual-sensors-dir> at :"
					+ getContainerFileName() + " is not valid.");
			return false;
		}
		file = new File(getWebAppPath());
		if (!file.exists() || !file.isDirectory()) {
			logger.error(" The path in the <webapp-location> at :"
					+ getContainerFileName() + " is not valid.");
			return false;
		}
		file = new File(getWrapperExtentionsPropertiesFile());
		if (!file.exists() || !file.isFile()) {
			logger.error(" The path in the <wrapper-extentions> at :"
					+ getContainerFileName() + " is not valid.");
			return false;
		}
		if (getMailServer() != null
				&& !ValidityTools.isAccessibleSocket(getMailServer(), getPort(
						getMailServer(), ValidityTools.SMTP_PORT)))
			return false;
		if (getSmsServer() != null
				&& !ValidityTools.isAccessibleSocket(getSmsServer(), getPort(
						getSmsServer(), ValidityTools.SMTP_PORT)))
			return false;
		return true;
	}

	private int getPort(String emailServer, int default_port) {
		if (emailServer == null || emailServer.length() < 3) {
			logger
					.warn("can't understand the value provided for the webEmail server");
			return default_port;
		}
		StringTokenizer stringTokenizer = new StringTokenizer(emailServer, ":");

		stringTokenizer.nextToken();// passing the hostname
		if (stringTokenizer.hasMoreTokens())
			try {
				return Integer.parseInt(stringTokenizer.nextToken());
			} catch (Exception e) {
				logger.warn(e.getMessage());
				logger
						.debug("can't convert the port number to the integer.",
								e);
			}
		return default_port;

	}

	/**
	 * @return Returns the smsPassword.
	 */
	public String getSmsPassword() {
		return smsPassword;
	}

	/**
	 * @param newSmsPassword
	 *            The smsPassword to set.
	 */
	public void setSmsPassword(String newSmsPassword) {
		this.smsPassword = newSmsPassword;
	}

	/**
	 * @return Returns the smsServer.
	 */
	public String getSmsServer() {
		return smsServer;
	}

	/**
	 * @param newSmsServer
	 *            The smsServer to set.
	 */
	public void setSmsServer(String newSmsServer) {
		this.smsServer = newSmsServer;
	}

	/**
	 * @return Returns the mailServer.
	 */
	public String getMailServer() {
		return mailServer;
	}

	/**
	 * @param mailServer
	 *            The mailServer to set.
	 */
	public void setMailServer(String mailServer) {
		this.mailServer = mailServer;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName()).append(" class [").append(
				"name=").append(webName).append(",");
		return builder.append("]").toString();
	}

}
