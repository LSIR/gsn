package gsn.beans;

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

    protected boolean permanentStorage = false;

    public static final String FIELD_NAME_permanentStorageEnabled = "permanentStorage";

    public boolean isPermanentStorage() {
	return this.permanentStorage;
    }

    public boolean isJdbcOverwriteTables() {
	return this.jdbcOverwriteTables;
    }

    public String getContainerFileName() {
	return this.containerFileName;
    }

    public void setContainerConfigurationFileName(final String containerFileName) {
	this.containerFileName = containerFileName;
    }

    public String getJdbcURL() {
	return this.jdbcURL;
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
	return this.jdbcDriver;
    }

    public String getJdbcUsername() {
	return this.jdbcUsername;
    }

    public String getJdbcPassword() {
	return this.jdbcPassword;
    }

    /**
         * @return Returns the containerPort.
         */
    public int getContainerPort() {
	return this.containerPort;
    }

    /**
         * @return Returns the webDescription.
         */
    public String getWebDescription() {
	if (this.webDescription == null
		|| this.webDescription.trim().equals(""))
	    this.webDescription = NOT_PROVIDED;
	return this.webDescription.trim();
    }

    /**
         * @return Returns the webEmail.
         */
    public String getWebEmail() {
	if (this.webEmail == null)
	    this.webEmail = NOT_PROVIDED;
	return this.webEmail;
    }

    /**
         * @return Returns the name.
         */
    public String getWebName() {
	if (this.webName == null || this.webName.trim().equals(""))
	    this.webName = NOT_PROVIDED;
	this.webName = this.webName.trim();
	return this.webName;
    }

    /**
         * @return Returns the registryBootstrapAddr.
         */
    private boolean isRegistryBootStrapAddrInitialized = false;

    /**
         * Returns null if the Registery bootstrap is not valid (e.g., null,
         * empty, ...)
         * 
         * @return
         */
    public String getRegistryBootstrapAddr() {
	if (!this.isRegistryBootStrapAddrInitialized) {
	    if (this.registryBootstrapAddr != null)
		this.registryBootstrapAddr = this.registryBootstrapAddr.trim();
	    this.isRegistryBootStrapAddrInitialized = true;
	}
	return this.registryBootstrapAddr;
    }

    /**
         * @param registryBootstrapAddr
         *                the registryBootstrapAddr to set
         */
    public void setRegistryBootstrapAddr(final String registryBootstrapAddr) {
	this.registryBootstrapAddr = registryBootstrapAddr;
    }

    /**
         * @return Returns the storagePoolSize.
         */
    public int getStoragePoolSize() {
	if (this.storagePoolSize <= 0)
	    this.storagePoolSize = DEFAULT_STORAGE_POOL_SIZE;
	return this.storagePoolSize;
    }

    public String toString() {
	final StringBuilder builder = new StringBuilder();
	builder.append(this.getClass().getName()).append(" class [").append(
		"name=").append(this.webName).append(",");
	return builder.append("]").toString();
    }

}
