package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.io.WriterOutputStream;

import com.thoughtworks.xstream.XStream;

public class RestDelivery implements DeliverySystem {

    private Continuation continuation;
    private ObjectOutputStream objectStream;

    public RestDelivery(Continuation connection) throws IOException {
        this.continuation = connection;
        XStream dataStream = StreamElement4Rest.getXstream();
        objectStream = dataStream.createObjectOutputStream((new WriterOutputStream(continuation.getServletResponse().getWriter())));
    }

    private static transient Logger logger = Logger.getLogger(RestDelivery.class);

    public void writeStructure(DataField[] fields) throws IOException {
        objectStream.writeObject(fields);
        objectStream.flush();
        continuation.getServletResponse().flushBuffer();
    }

    public boolean writeStreamElement(StreamElement se) {
        try {
            continuation.resume();
            Semaphore lock = (Semaphore) continuation.getAttribute("lock");
            lock.acquire();
            objectStream.writeObject(new StreamElement4Rest(se));
            objectStream.flush();
            if (continuation.getServletResponse().getWriter().checkError()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            return false;
        }
    }


    public void close() {
        try {
            if (objectStream != null){
                continuation.complete();
                objectStream.close();
            }
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }

    }

    public boolean isClosed() {
        try {
            return continuation.getServletResponse().getWriter().checkError();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

    }
}
