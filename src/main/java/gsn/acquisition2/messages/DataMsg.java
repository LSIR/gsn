/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/acquisition2/messages/DataMsg.java
*
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn.acquisition2.messages;

public class DataMsg extends AbstractMessage {

  private static final long serialVersionUID = 6707634030386675571L;

  private Object[] data;

  private long sequenceNumber = -1;
  
private long created_at = -1;
  
  public long getSequenceNumber() {
    return sequenceNumber;
  }

  public Object[] getData() {
    return data;
  }

  public long getCreated_at() {
    return created_at;
  }

  public DataMsg(Object[] data,long seqNo,long created_at) {
    this.data = data;
    this.sequenceNumber=seqNo;
    this.created_at = created_at;
  }
  
}
