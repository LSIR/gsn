package gsn.beans;

public class AccessControlConfig {

    private StorageConfig storage = null;
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }
}
