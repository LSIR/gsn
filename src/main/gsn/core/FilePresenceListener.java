package gsn.core;

public interface FilePresenceListener {

  public void fileRemoval(String filePath);

  public void fileAddition(String filePath);

  public void fileChanged(String filePath);
}
