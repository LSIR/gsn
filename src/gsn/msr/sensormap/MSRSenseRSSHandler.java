package gsn.msr.sensormap;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.ContainerInfoHandler;
import gsn.http.HttpRequestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.W3CGeoModuleImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
/**
 * rss?vsname=nameOFVS&locatable=true
 * @author salehi
 *
 */
public class MSRSenseRSSHandler extends HttpServlet{
  
  private static transient Logger logger             = Logger.getLogger( MSRSenseRSSHandler.class );
  
  private static final String MIME_TYPE = "application/xml; charset=UTF-8";
  
  public void doGet ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
    response.setContentType(MIME_TYPE);
    SyndFeed feed = getFeed(request);
    SyndFeedOutput output = new SyndFeedOutput();
    try {
      output.output(feed,response.getWriter());
    } catch (FeedException e) {
      logger.error(e.getMessage(),e);
    }
  }
  //http://localhost:22001/rss?vsname=memoryusage5&locatable=true
  protected SyndFeed getFeed(HttpServletRequest req) {
    SyndFeed feed = new SyndFeedImpl();
    feed.setFeedType("rss_2.0");
    feed.setTitle(Main.getContainerConfig().getWebName());
    feed.setDescription(Main.getContainerConfig().getWebDescription());
    feed.setAuthor(Main.getContainerConfig().getWebAuthor());
    feed.setLink("http://"+Main.getContainerConfig().getContainerPort()+"/rss");
    feed.setDescription("description of this GSN node");
    ArrayList<SyndEntry> entries = new ArrayList<SyndEntry>();
    // http://localhost:22001/rss?vsname=invmaccess
    Boolean location_required = HttpRequestUtils.getBooleanParameter("locatable", false, req);
    VSensorConfig config = Mappings.getVSensorConfig(req.getParameter("vsname"));//if missing, means that all virtual sensors.
    if (config==null) {
      Iterator<VSensorConfig> it = Mappings.getAllVSensorConfigs();
      while(it.hasNext()) {
        SyndEntry generateFeed = generateFeed(it.next(),location_required);
        if (generateFeed!=null)
          entries.add(generateFeed);
      }
    }else {
      SyndEntry generateFeed = generateFeed(config,location_required);
      if (generateFeed!=null)
        entries.add(generateFeed);
    }
    feed.setEntries(entries);
    return feed;
  }
  
  private SyndEntry generateFeed(VSensorConfig conf,boolean locatable) {
    SyndEntry entry = new SyndEntryImpl();
    entry.setTitle(conf.getName());
    
    
//  entry.setLink("http://localhost:22001/");
    SyndContent  description = new SyndContentImpl();
    description.setType("text/html");
    entry.setDescription(description);
    
//  description.setValue("<![CDATA["+"TEST"+"]]>");
    ArrayList<StreamElement> values = ContainerInfoHandler.getMostRecentValueFor(conf.getName());
    StringBuilder formattedOutput = new StringBuilder();
    formattedOutput.append("<table class=\"gsn\"><thead><tr><td>Sensor Name</td><td>Value</td></tr></thead><tbody>");
    boolean hasValue =(values!=null&&!values.isEmpty()); 
    for (StreamElement se:values) {
      for ( DataField df : conf.getOutputStructure( ) ) { 
        formattedOutput.append("<tr><td>").append(df.getName()).append("</td><td>");
        if (!hasValue) {
          formattedOutput.append("No data available.</td></tr>");
          continue;
        }
        if (df.getDataTypeID()==DataTypes.BINARY && df.getType().toLowerCase().indexOf("image")>=0)
          formattedOutput.append("<img src=\"").append(se.getData(df.getName())).append("\" />");
        else if (df.getDataTypeID()==DataTypes.BINARY)
          formattedOutput.append("<a href=\"").append(se.getData(df.getName())).append("\" >Download</a>");
        else   
          formattedOutput.append(se.getData(df.getName()));
        formattedOutput.append("</td></tr>");
      }
      entry.setPublishedDate(new Date(values.get(0).getTimeStamp()));
    }
    formattedOutput.append("</tbody></table>");
    description.setValue(formattedOutput.toString());
    
    if (conf.getLatitude()!=null && conf.getLongitude()!=null ) {//checks to see if the sensor is locatable.
      GeoRSSModule geoRSSModule = new W3CGeoModuleImpl();      
      geoRSSModule.setPointLatitude(conf.getLatitude());        // Set up a point
      geoRSSModule.setPointLongitude(conf.getLongitude());
      if (conf.getAltitude()!=null)
        geoRSSModule.setElevation(conf.getAltitude());        // Set up the elevation information
      entry.getModules().add(geoRSSModule);        // Add this element to the entry object  
    }else if (locatable) { //comes here when the sensor is not locatable but request desires locatable sensors.
      return null;
    }
    return entry;
  }
}
