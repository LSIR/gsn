package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;

import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;

/**
 * Virtual sensor for sending email. 
 * 
 * This class requires at least javamail version 1.2, which is not packed with GSN. 
 * Get it from http://java.sun.com/products/javamail/
 * 
 * Receiver's e-mail address can be defined either in the VS's configuration
 * parameters, or it can be get from the datastream.
 */
public class EmailVirtualSensor implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}


	private static final transient Logger logger = Logger.getLogger( EmailVirtualSensor.class );
	private static final String		DEFAULT_SUBJECT = "GSN-Notification";
	/*
	 * These values are used when extracting receiver's email-address and the message to
	 * be sent from a datastream.  
	 */
	private static final String		RECEIVER_FIELD_NAME = "RECEIVER";
	private static final String		MESSAGE_FIELD_NAME = "MESSAGE";
	/*
	 * These values must match to the parameter names in VirtualSensor's configuration file.
	 */
	private static final String		INITPARAM_SENDER = "sender-email";
	private static final String		INITPARAM_RECEIVER = "receiver-email";
	private static final String		INITPARAM_SERVER = "mail-server";
	private static final String		INITPARAM_SUBJECT = "subject";

	private String					subject    = DEFAULT_SUBJECT;
	private String					receiverEmail = "";
	private String					senderEmail;
	private String					mailServer;
	private SimpleEmail 			email;
	private DataChannel outputChannel;

	public  EmailVirtualSensor (OperatorConfig config,DataChannel outputChannel ) throws EmailException {
		this.outputChannel = outputChannel;
		subject = config.getParameters().getValueWithDefault(INITPARAM_SUBJECT,DEFAULT_SUBJECT);
		receiverEmail = config.getParameters().getValueWithDefault(INITPARAM_RECEIVER,"");
		senderEmail =config.getParameters().getValueWithException(INITPARAM_SENDER);
		mailServer =config.getParameters().getValueWithException(INITPARAM_SERVER);

		email = new SimpleEmail();
		email.setHostName(mailServer);
		email.setFrom(senderEmail);
		email.setSubject( subject );
	}

	public void process ( String inputStreamName , StreamElement data ) {
		String [ ] fieldNames = data.getFieldNames( );
		String message = "";

		for(int i=0; i < fieldNames.length; i++) {
			String fn = fieldNames[i];
			if(fn.equals(RECEIVER_FIELD_NAME)) {
				receiverEmail = (String) data.getValue(fn);
			} else if(fn.equals(MESSAGE_FIELD_NAME)) {
				message = (String) data.getValue(fn);
			}
		}

		if(message.equals("") == false) { 
			send(message);
		}
	}

	/*
	 * Sends the previously formatted e-mail.
	 * Because there are two ways to define receiver's address, this method
	 * has to check if the address actually exists.
	 */
	private boolean send ( String message ) {
		try {
			if(receiverEmail.equals("")) {
				logger.error("Sending e-mail failed: no receiver.");
				return false;
			}
			email.addTo(receiverEmail);
			email.setContent( message , "text/plain" );
			email.send( );         
		} catch ( Exception e ) {
			logger.error( "Sending e-mail failed, trying to send to *" + receiverEmail + "*\n", e );
			return false;
		}
		return true;
	}

	public void dispose ( ) {

	}
	
}
