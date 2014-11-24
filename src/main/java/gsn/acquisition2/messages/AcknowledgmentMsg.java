/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/gsn/acquisition2/messages/AcknowledgmentMsg.java
*
* @author Ali Salehi
*
*/

package gsn.acquisition2.messages;
/**
 * Represents Ack (success) and Nack (failure) messages.
 */
public class AcknowledgmentMsg extends AbstractMessage {
  
  private static final long serialVersionUID = 3096327899834535287L;

  public static transient final int SUCCESS = 1;  
  public static transient final int FAILURE = 2;
  
  private long seqNumber = -1;
  
  private int value = -1;
  
  public boolean isAck() {
    return value == SUCCESS;
  }
  
  public boolean isNack() {
    return value == FAILURE;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + value;
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final AcknowledgmentMsg other = (AcknowledgmentMsg) obj;
    if (value != other.value)
      return false;
    return true;
  }

  public AcknowledgmentMsg(int messageType,long seqNo) {
    this.value = messageType;
    this.seqNumber=seqNo;
  }
  
  public AcknowledgmentMsg(int messageType) {
    this.value = messageType;
  }

  public long getSeqNumber() {
    return seqNumber;
  }
  
 
}
