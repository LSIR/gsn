package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.IOException;
import java.io.ObjectOutputStream;

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

	private static transient Logger       logger     = Logger.getLogger ( RestDelivery.class );

	public void writeStructure(DataField[] fields) throws IOException {
		objectStream.writeObject(fields);
		objectStream.flush();
		continuation.getServletResponse().flushBuffer();
		continuation.setAttribute("2ndPass", Boolean.TRUE);
		continuation.resume();
	}

	public boolean writeStreamElement(StreamElement se) {
		try {
			objectStream.writeObject(new StreamElement4Rest(se));
			objectStream.flush();
			continuation.getServletResponse().flushBuffer();
			continuation.setAttribute("error", Boolean.valueOf(continuation.getServletResponse().getWriter().checkError()));
			continuation.resume();
			if (continuation.getServletResponse().getWriter().checkError()) {
				return false;
			}
			return true;
		} catch (Exception e) {
			logger.debug(e.getMessage(),e);
			return false;
		}

	}

	public void close() {
		try {
			if (objectStream!=null)
				objectStream.close();
		} catch (IOException e) {
			logger.debug(e.getMessage(),e);
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
