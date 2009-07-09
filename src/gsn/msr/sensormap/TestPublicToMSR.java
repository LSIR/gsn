package gsn.msr.sensormap;

import gsn.beans.ContainerConfig;
import gsn.beans.VSensorConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.apache.log4j.Logger;

public class TestPublicToMSR {
  
  private static final String HTTP_RESEARCH_MICROSOFT_COM_NEC = "http://research.microsoft.com/nec/";
  
  private static transient Logger logger             = Logger.getLogger( TestPublicToMSR.class );
  
  public static boolean register_to_sensor_map(String user,String password,String host,ContainerConfig container_conf ,VSensorConfig conf) throws SOAPException, IOException {
    URL wsdl = new URL("http://atom.research.microsoft.com/sensordatahub/service.asmx?WSDL");
    QName service = new QName(HTTP_RESEARCH_MICROSOFT_COM_NEC, "DataHub");
    QName port = new QName(HTTP_RESEARCH_MICROSOFT_COM_NEC, "DataHubSoap12");
    
    // service & dispatch lookup/creation
    Service ws = Service.create(wsdl, service);
    Dispatch<SOAPMessage> dispatch = ws.createDispatch(port, SOAPMessage.class, Service.Mode.MESSAGE);
    
    MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
    SOAPMessage msg = mf.createMessage();
    msg.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
    SOAPPart soapPart=msg.getSOAPPart();
    SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
    Name name = soapEnvelope.createName("RegisterSensor2", null, HTTP_RESEARCH_MICROSOFT_COM_NEC);
    SOAPBodyElement soapBodyElement = msg.getSOAPBody().addBodyElement(name);
    // Creating the MSRSensrDescriptor Bean.
    MSRSensorDescriptor sensorDescriptor = new MSRSensorDescriptor();
    sensorDescriptor.setPublisherName(container_conf.getWebAuthor()+" (email: "+container_conf.getWebEmail()+")");
    sensorDescriptor.setSensorName(conf.getName());
    sensorDescriptor.setLatitude(conf.getLatitude());
    sensorDescriptor.setLongitude(conf.getLongitude());
    sensorDescriptor.setAltitude(conf.getAltitude());
    sensorDescriptor.setSensorType(MSRSensorDescriptor.SENSOR_TYPE_GEORSS);
    sensorDescriptor.setDataType(MSRSensorDescriptor.DATA_TYPE_HTML);
    sensorDescriptor.setUnit("Custom");
    sensorDescriptor.setSamplingPeriod(1);
    sensorDescriptor.setReportPeriod(1);
    sensorDescriptor.setDescription(container_conf.getWebDescription());
    sensorDescriptor.setKeywords("");
    sensorDescriptor.setUrl("http://"+host+"/rss%3flocatable=true%26vsname="+conf.getName());// GeoRSS feed.
    
    // user info part.
    SOAPElement soapElement = soapBodyElement.addChildElement(soapEnvelope.createName("publisherName"));
    soapElement.addTextNode(user);
    soapElement = soapBodyElement.addChildElement(soapEnvelope.createName("password"));
    soapElement.addTextNode(password);
    soapElement = soapBodyElement.addChildElement(soapEnvelope.createName("sensorDescription"));
    soapElement.addTextNode(conf.getDescription()); 
    // Sensor detail part.
    soapElement = soapBodyElement.addChildElement(soapEnvelope.createName("Sensor"));
    soapElement.addChildElement("publisherName").addTextNode(sensorDescriptor.getPublisherName());
    soapElement.addChildElement("sensorName").addTextNode(sensorDescriptor.getSensorName());
    soapElement.addChildElement("longitude").addTextNode(Double.toString(sensorDescriptor.getLongitude()));
    soapElement.addChildElement("latitude").addTextNode(Double.toString(sensorDescriptor.getLatitude()));
    if (sensorDescriptor.getAltitude()!=null)
      soapElement.addChildElement("altitude").addTextNode(Double.toString(sensorDescriptor.getAltitude()));
    soapElement.addChildElement("unit").addTextNode(sensorDescriptor.getUnit());
    soapElement.addChildElement("sensorType").addTextNode(sensorDescriptor.getSensorType());
    soapElement.addChildElement("url").addTextNode(sensorDescriptor.getUrl());
    soapElement.addChildElement("keywords").addTextNode("Sensor Network published by GSN.");
    soapElement.addChildElement("description").addTextNode(sensorDescriptor.getDescription());
    soapElement.addChildElement("dataType").addTextNode(sensorDescriptor.getDataType());
    soapElement.addChildElement("samplingPeriod").addTextNode(Integer.toString(sensorDescriptor.getSamplingPeriod()));
    soapElement.addChildElement("reportPeriod").addTextNode(Integer.toString(sensorDescriptor.getReportPeriod()));
    msg.saveChanges();
    ByteArrayOutputStream req_log_out = new ByteArrayOutputStream();
    msg.writeTo(req_log_out);
    // invoke
    SOAPMessage response = dispatch.invoke(msg);
    req_log_out.write('\n');
    response.writeTo(req_log_out);
    String req_log_str = req_log_out.toString();
    if (req_log_str.indexOf("OK")>0 ) {
     logger.warn("Registeration of virtual sensor: "+conf.getName()+" to the microsoft sensor map done successfully."+req_log_str);
     logger.debug(req_log_str);
    }else if ( req_log_str.indexOf("ERROR")>0 && req_log_str.indexOf("same name and publisher exists")>0 ) {
      logger.warn("(re)Registeration of virtual sensor: "+conf.getName()+"."+req_log_str);
     // logger.debug(req_log_str);
    }else {//there is an error
    logger.warn("Registeration to the sensor map failed, here are the request and respond");
    logger.warn(req_log_str);
    }
    
    return true;
  }
  
}
