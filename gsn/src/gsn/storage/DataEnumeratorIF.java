package gsn.storage;

import gsn.beans.StreamElement;

import java.util.Enumeration;

public interface DataEnumeratorIF extends Enumeration<StreamElement> {

    public boolean hasMoreElements() ;

    public StreamElement nextElement() throws RuntimeException ;

    public void close() ;

}
