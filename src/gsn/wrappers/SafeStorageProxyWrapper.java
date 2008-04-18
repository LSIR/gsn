package gsn.wrappers;

import gsn.beans.*;

import java.io.*;

public abstract class SafeStorageProxyWrapper extends AbstractWrapper{

  protected void postStreamElement(long timestamp, Serializable... values) {
  }

  protected void postStreamElement(Serializable... values) {
  }

}
