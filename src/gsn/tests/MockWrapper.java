package gsn.tests;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

public class MockWrapper extends AbstractWrapper{

  private boolean finalizedCalled = false;
  private boolean initializedCalled = false;
  private ArrayList<StreamElement> streamElements= new ArrayList<StreamElement>();
  private boolean releaseResourcesCalled=false;

  private DataField[] outputStructure;
  private int dbAlias;


  public boolean isFinalizedCalled() {
    return finalizedCalled;
  }

  public void setFinalizedCalled(boolean finalizedCalled) {
    this.finalizedCalled = finalizedCalled;
  }

  public boolean isInitializedCalled() {
    return initializedCalled;
  }

  public void setInitializedCalled(boolean initializedCalled) {
    this.initializedCalled = initializedCalled;
  }

  public DataField[] getOutputStructure() {
    return outputStructure;
  }

  public void setOutputStructure(DataField[] outputStructure) {
    this.outputStructure = outputStructure;
  }

  public void finalize() {
    finalizedCalled = true;
  }

  public DataField[] getOutputFormat() {
    return outputStructure;
  }

  public String getWrapperName() {
    return null;
  }

  public boolean initialize() {
    return (initializedCalled  =true);
  }


  protected void postStreamElement(long timestamp, Serializable[] values) {
    StreamElement se = new StreamElement(getOutputFormat(),values,timestamp);
    streamElements.add(se); 
  }

  protected void postStreamElement(Serializable... values) {
    StreamElement se = new StreamElement(getOutputFormat(),values,System.currentTimeMillis());
    streamElements.add(se); 
  }

  protected Boolean postStreamElement(StreamElement se) {
    streamElements.add(se);
    return true;
  }

  public ArrayList<StreamElement> getStreamElements() {
    return streamElements;
  }

  public void releaseResources() throws SQLException {
    releaseResourcesCalled=true;
  }

  public boolean isReleaseResourcesCalled() {
    return releaseResourcesCalled;
  }

  public void setReleaseResourcesCalled(boolean releaseResourcesCalled) {
    this.releaseResourcesCalled = releaseResourcesCalled;
  }

  public int getDBAlias() {
    return dbAlias;
  }

  public int getDbAlias() {
    return dbAlias;
  }

  public void setDbAlias(int dbAlias) {
    this.dbAlias = dbAlias;
  }

  public boolean manualDataInsertion(StreamElement se) {
    return postStreamElement(se);
  }

}
