package gsn.wrappers.general;

import gsn.Main;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class OstLuftWrapper extends AbstractWrapper {
   
	private int						DEFAULT_RATE		= 12*60*60*1000;  // 12h in ms

	private final transient Logger	logger				= Logger.getLogger( OstLuftWrapper.class );

	private URL						url;
	private int						rate;
	private Integer				deviceId			= null;

	private static int				threadCounter		= 0;
	private SimpleDateFormat format    = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat formatTable = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	private long last_db_timestamp = 0;
	private String urlPath;
	
	private DataField [] outputStructure = new DataField [] {
	    new DataField("DEVICE_ID", "INTEGER"),
      new DataField("GENERATION_TIME", "BIGINT"),
      new DataField("OZONE_PPB", "DOUBLE"),
      new DataField("CO_PPM", "DOUBLE"),
      new DataField("NO2_PPB", "DOUBLE"),
      new DataField("TEMPERATURE_C", "DOUBLE")};

	/**
	* From XML file it needs the followings :
	* <ul>
	* <li>url</li> The full url for retriving the data. The starting time has to be replaced with the string STARTZEIT and ending time with ENDZEIT
	* <li>rate</li> The interval in msec for updating/asking for new information.
	* <li>device-id</li> The device id of the data producer.
	* </ul>
	*/
	public boolean initialize (  ) {
		AddressBean addressBean =getActiveAddressBean( );
		urlPath = addressBean.getPredicateValue( "url" );
		
		formatTable.setTimeZone( TimeZone.getTimeZone( "cest" ) );
		
		String id = getActiveAddressBean().getPredicateValue("device-id");
		if (id != null) {
			try {
				deviceId = Integer.parseInt(id);
			} catch (NumberFormatException e) {
				logger.error("device id >" + id + "< is not an integer");
				return false;
			}
			
			if (deviceId < 0 || deviceId > 65535) {
				logger.error("device id >" + id + "< has to be between 0 and 65535");
				return false;
			}
		}
		else {
		  logger.error("device id is missing");
      return false;
		}
		
		String inputRate = addressBean.getPredicateValue( "rate" );
		if ( inputRate == null || inputRate.trim( ).length( ) == 0 )
			rate = DEFAULT_RATE;
		else
			rate = Integer.parseInt( inputRate );
 
		setName( "OstLuftReceiver-Thread" + ( ++threadCounter ) );
		logger.info("OstLuftWrapper is now running @" + rate + " rate");
		return true;
	}

	public void run ( ) {
	  
	  while ( isActive() ) {
      
      // Get the latest timestamp from the database
      Connection conn = null;
      ResultSet rs = null;
      try {
        conn = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).getConnection();
        StringBuilder query = new StringBuilder();
        query.append("select generation_time from ").append(getActiveAddressBean().getVirtualSensorConfig().getName()).append(" order by generation_time desc limit 1");
        rs = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).executeQueryWithResultSet(query, conn);
        
        if (rs.next()) {
          // get latest timestamp
          last_db_timestamp = rs.getLong("generation_time");
        } else {
          // Set it to 48h before
          last_db_timestamp = System.currentTimeMillis()-48*3600000;
          logger.warn("no timestamp available in the database, start with " + format.format(new java.util.Date(last_db_timestamp-3600000)));
        }
        rs.close();
        conn.close();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
      
      // start at the latest db entry - 60min
      String starttime = format.format(new java.util.Date(last_db_timestamp-3600000));
      String endtime = format.format(new java.util.Date(System.currentTimeMillis()));
      
      // Replace strings
      String currUrlPath = urlPath;
      currUrlPath = currUrlPath.replace("STARTTIME", starttime);
      currUrlPath = currUrlPath.replace("ENDTIME", endtime);
      logger.debug("get ostluft data from " + starttime + " to " + endtime);
      
      try {
        url = new URL(currUrlPath);
      } catch (MalformedURLException e) {
        logger.error("Loading the http wrapper failed : "+e.getMessage(),e);
        return;
      }
      
      BufferedReader content;
      
      try {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.connect();
        if ( httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED ) return;
  
        String decodedString;
        content = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
  
        while ((decodedString = content.readLine()) != null) {
          
          HtmlTableParser parser = new HtmlTableParser();
          parser.parse(decodedString);
          
          Vector<Vector<String>> contents = parser.getContents();
          for(int i=0; i<contents.size(); i++) {
            try {
              
              // Check that vector has exactly four elements.
              if (contents.get(i).size() != 5) continue;
              
              Date newDate = formatTable.parse((String) contents.get(i).get(0));
              // Go from cest (UTC+2h) to time format on the core station (UTC+1h)
              if (newDate.getTime() - 3600000 <= last_db_timestamp) continue;
              
              Double ozone = null;
              Double co = null;
              Double no2 = null;
              Double temp = null;
              
              if (((String) contents.get(i).get(1)).indexOf("-") < 0) {
                ozone = new Double((String) contents.get(i).get(1));
                ozone = ozone/2.0;
              }
              if (((String) contents.get(i).get(2)).indexOf("-") < 0) {
                co = new Double((String) contents.get(i).get(2));
                co = co/1.16;
              }
              if (((String) contents.get(i).get(3)).indexOf("-") < 0) {
                no2 = new Double((String) contents.get(i).get(3));
                no2 = no2/1.91;
              }
              if (((String) contents.get(i).get(4)).indexOf("-") < 0) {
                temp = new Double((String) contents.get(i).get(4));
              }
              
              // Only post stream if one of the values are not null
              if (ozone != null || co != null || no2 != null || temp != null)
                postStreamElement(new Serializable[]{deviceId, newDate.getTime() - 3600000, ozone, co, no2, temp});
              
            } catch (java.text.ParseException e){
              logger.error("Format parse exception: " + e.getMessage());
            }
          }
        }
        content.close();
  
      } catch (IOException e) {
        logger.error(e.getMessage( ) + " (host=" + url.getHost() + ")");
      }

			try {
				Thread.sleep( rate );
			} catch ( InterruptedException e ) {
				logger.error( e.getMessage( ) , e );
			}
	  }
	}
	
	public String getWrapperName() {
		return "OstLuft HttpWrapper";
	}

	public void dispose (  ) {
		threadCounter--;
	}

	public  DataField[] getOutputFormat ( ) {
		return outputStructure;
	}
	
	static class HtmlTableParser {
	  private Vector<Vector<String>> contents;
    private int maxsize;

    public HtmlTableParser() {}

    public void reset() {
      contents = new Vector<Vector<String>>();
      maxsize = 0;
    }

    public void parse(String htmlContent) {
      reset();

      // skip first two <th
      int start = htmlContent.indexOf("<th ", 0);
      start = htmlContent.indexOf("<th ", start+1);
      start = htmlContent.indexOf("<th ", start+1);
      start = htmlContent.indexOf("<th ", start+1);
      start = htmlContent.indexOf("<th ", start+1);

      for(int i=start+1; i<htmlContent.length(); i++) {
        // Search <th
        int idxTr = htmlContent.indexOf("<th ", i);
        if (idxTr < 0)
          break;
        i=idxTr;

        Vector<String> row = new Vector<String>();

        // Search </th
        int idxTrEnd =  htmlContent.indexOf("<\\/th", i);
        if (idxTrEnd < 0)
          idxTrEnd = htmlContent.length();

        String cell1Content = htmlContent.substring(idxTr,idxTrEnd);
        row.add(cell1Content.replaceAll("\\<.*?\\>", ""));

        // For O3, CO, NO2, and temp.
        for (int u = 0; u < 4; u++) {
          // Search <td
          int idxTd = htmlContent.indexOf("<td ", i);
          if (idxTd < 0)
            break;
          i=idxTd;
  
          // Search </td
          int idxTdEnd =  htmlContent.indexOf("<\\/td", i);
          if (idxTdEnd < 0)
            idxTdEnd = htmlContent.length();
          i=idxTdEnd;
          
          String cell2Content = htmlContent.substring(idxTd,idxTdEnd);
          row.add(cell2Content.replaceAll("\\<.*?\\>", ""));
        }

        if (row.size() > maxsize) {
          maxsize = row.size();
        }
        contents.add(row);
      }
    }

    public Vector<Vector<String>> getContents() {
      return contents;
    }

    public String[][] getContentsArray() {
      String[][] contentsArray = new String[contents.size()][maxsize];
      for(int i=0; i<contents.size(); i++) {
        for(int j=0; j<contents.get(i).size(); j++) {
          contentsArray[i][j] = (String) contents.get(i).get(j);
        }
      }
      return contentsArray;
    }
	}
	
}
