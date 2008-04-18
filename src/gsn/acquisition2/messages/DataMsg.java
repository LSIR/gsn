package gsn.acquisition2.messages;

import java.io.*;

public class DataMsg extends AbstractMessage {

  private static final long serialVersionUID = 6707634030386675571L;

  private Serializable data;

  private long sequenceNumber = -1;
  
private long created_at = -1;
  
  public long getSequenceNumber() {
    return sequenceNumber;
  }

  public Serializable getData() {
    return data;
  }

  public long getCreated_at() {
    return created_at;
  }

  public DataMsg(Serializable data,long seqNo,long created_at) {
    this.data = data;
    this.sequenceNumber=seqNo;
    this.created_at = created_at;
  }
  
}
