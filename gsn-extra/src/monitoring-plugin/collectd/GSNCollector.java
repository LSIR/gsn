/**
 * collectd - bindings/java/org/collectd/java/JMXMemory.java
 * Copyright (C) 2009       Florian octo Forster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Authors:
 *   Florian octo Forster <octo at collectd.org>
 */

package collectd;

import java.util.List;
import java.util.Scanner;
import java.util.Date;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.MalformedURLException;

import org.collectd.api.Collectd;
import org.collectd.api.DataSet;
import org.collectd.api.ValueList;
import org.collectd.api.Notification;
import org.collectd.api.OConfigItem;

import org.collectd.api.CollectdConfigInterface;
import org.collectd.api.CollectdInitInterface;
import org.collectd.api.CollectdReadInterface;
import org.collectd.api.CollectdShutdownInterface;

import org.collectd.api.OConfigValue;
import org.collectd.api.OConfigItem;

public class GSNCollector implements CollectdConfigInterface,
       CollectdInitInterface,
       CollectdReadInterface,
       CollectdShutdownInterface {

    private URL url;

    public GSNCollector () {
            
        Collectd.registerConfig   ("GSNCollector", this);
        Collectd.registerInit     ("GSNCollector", this);
        Collectd.registerRead     ("GSNCollector", this);
        Collectd.registerShutdown ("GSNCollector", this);
        url = null;
    }

  private void submit (String metric, double value) /* {{{ */
  {
    ValueList vl;

    vl = new ValueList();

    vl.setHost(getHost()); 
    
    if (metric.endsWith("counter")){ // type could be gauge OR counter. It is always at the end of the string
	    vl.setPlugin("gsn."+metric.substring(0, metric.length()-8));        
	    vl.setType("counter");           
    }else if(metric.endsWith("gauge")){
	    vl.setPlugin("gsn."+metric.substring(0, metric.length()-6));        
	    vl.setType("gauge");  
    }else{
    	Collectd.logError ("GSNCollector plugin: Unknown metric type for GSNCollector in '"+metric+"'");
    }
    
    vl.addValue(value);
    Collectd.dispatchValues(vl);
    vl.clearValues();
    
  } /* }}} void submit */

  private int setURL (OConfigItem ci) {
  
    List <OConfigValue> values;
    OConfigValue cv;

    values = ci.getValues();

    if (values.size() != 1) {
        Collectd.logError ("GSNCollector plugin: URL option needs exactly " 
                + "one string argument");
        return (-1);
    }
    
    cv = values.get(0);

    if (cv.getType() != OConfigValue.OCONFIG_TYPE_STRING) {
        
        Collectd.logError ("GSNCollector plugin: URL option needs exactly " 
                + "one string argument");
        return (-1); 
    }
    
    String rawURL = cv.getString().trim();

    try {
        this.url = new URL (rawURL);
    } catch (MalformedURLException e) { //TODO: Proper exception handling
        Collectd.logError("GSNCollector plugin: Invalid URL format " + rawURL.trim());
    }
    
    return 0;
  }

  public int config (OConfigItem ci)
  {
    
    List<OConfigItem> children;
    int i;

    Collectd.logInfo ("GSNCollector plugin: config: ci = " + ci + ";");

    children = ci.getChildren ();
    for (i = 0; i < children.size (); i++)
    {
      OConfigItem child;
      String key;

      child = children.get (i);
      key = child.getKey ();
      if (key.equalsIgnoreCase ("URL"))
      {
        setURL (child);
        //TODO : Check logError output | Where does it go? 
      }
      else
      {
        Collectd.logError ("GSNCollector plugin: Unknown config option: " + key);
      }
    }

    return (0);
  } 

  public int init ()
  { 
    Collectd.logInfo("In init()");
    return (0);
  } 

    public int read () {
        
        try {
            Scanner s = new Scanner (url.openStream());
            
            while (s.hasNextLine()) {
                String line = s.nextLine();
                String metric = line.trim();
                String [] parts = metric.split (" ");

                if (parts.length  != 2) {
                    Collectd.logError ("GSNCollector plugin: Metric format invalid");
                    continue;
                }
                
                String name = parts[0];
                String val = parts[1];
                try{
                	double value = Double.parseDouble(val);
                	submit(name,value);
                }catch (NumberFormatException e){
                	Collectd.logError ("GSNCollector plugin: unable to parse metric value: "+val);
                }
            }

        } catch (UnknownHostException e) { 
        	Collectd.logError ("GSNCollector plugin: unable to connect to GSN server: "+url);
        } catch (IOException e) {
        	Collectd.logError ("GSNCollector plugin: unable to connect to GSN server: "+url);
        }
    
    return (0);
  }

  public int shutdown () 
  {
    Collectd.logInfo ("org.collectd.java.GSNCollector.Shutdown ();\n");
    return (0);
  }

    private String getHost() {

      try {
    	    String hostname = InetAddress.getLocalHost().getHostName().replaceAll("\\.", "_");
            return hostname;
      } catch (UnknownHostException e) {
            return "unknownhost";
      }
      //return hostname;
    }

}

