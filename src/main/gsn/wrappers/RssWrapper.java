package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/*
 * This wrapper allows GSN to extract Rss Feed from a given URL of Rss Feed.
 * Gets one parameter called url
 * Output has three fields (title[varchar[100]], author[varchar[100]],description[varchar[255]],link[varchar[255]]). 
 */
public class RssWrapper implements Wrapper {

  private final WrapperConfig conf;

  private final DataChannel dataChannel;


  private int                      SAMPLING_RATE_IN_MSEC       = 60000; //every 60 seconds.

  private final transient Logger   logger             = Logger.getLogger( RssWrapper.class );


  private int                      rate                  ;

  private URL                      url                   ;

  private SyndFeedInput            rss_input             ;

  private SyndFeed                 feed                  ;

  private transient final DataField [] outputStructure = new  DataField [] { new DataField( "title" , "string" ),new DataField("author","string"),new DataField("description","string"),new DataField("link","String")};

  public RssWrapper(WrapperConfig conf, DataChannel channel) throws MalformedURLException {
    this.conf = conf;
    this.dataChannel= channel;
    url = new URL(conf.getParameters().getPredicateValueWithException("url"));
    rate = conf.getParameters().getPredicateValueAsInt( "rate" ,SAMPLING_RATE_IN_MSEC);
    logger.debug( "RssWrapper is now running @" + rate + " Rate." );
  }

  public void start(){
    while ( isActive ) {
      try {
        Thread.sleep( rate );
        rss_input = new SyndFeedInput();
        feed = rss_input.build(new XmlReader(url));
        for (SyndEntry entry: (List<SyndEntry>) feed.getEntries()) {
          String title = entry.getTitle();
          String link = entry.getLink();
          String description= entry.getDescription().getValue();
          String author = entry.getAuthor();
          long publish_date = entry.getPublishedDate().getTime();

          Serializable[] values = {title, author, description, link};
          StreamElement se = StreamElement.from(this).setTime(publish_date);
          for (int i=0;i<getOutputFormat().length;i++)
            se.set(getOutputFormat()[i].getName(),values[i]);

          dataChannel.write(se);
        }
      }catch (com.sun.syndication.io.FeedException e){
        logger.error( e.getMessage( ) , e );
      }catch ( InterruptedException e ) {
        logger.error( e.getMessage( ) , e );
      }catch (IOException e) {
        logger.error( e.getMessage( ) , e );
      }
    }
  }

  private boolean isActive=true;

  public void dispose ( ) {

  }

  public  DataField[] getOutputFormat ( ) {
    return outputStructure;
  }

  public void stop() {
    isActive = false;
  }
}
