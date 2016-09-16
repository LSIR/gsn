/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: src/ch/epfl/gsn/wrappers/general/HttpGetWrapper.java
*
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package ch.epfl.gsn.wrappers.general;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.wrappers.AbstractWrapper;

import org.slf4j.Logger;

public class HttpGetWrapper extends AbstractWrapper {
   
   private int                      DEFAULT_RATE       = 2000;
   
   private static int               threadCounter      = 0;
   
   private final transient Logger   logger             = LoggerFactory.getLogger( HttpGetWrapper.class );
   
   private String                   urlPath;
   
   private HttpURLConnection httpURLConnection;
   
   private URL url;
   
  private AddressBean              addressBean;
   
   private String                   inputRate;
   
   private int                      rate;
   
   private transient final DataField [] outputStructure = new  DataField [] { new DataField( "data" , "binary:image/jpeg" , "JPEG image from the remote networked camera." ) };
   
  
   /**
    * From XML file it needs the followings :
    * <ul>
    * <li>url</li> The full url for retriving the binary data.
    * <li>rate</li> The interval in msec for updating/asking for new information.
    * <li>mime</li> Type of the binary data.
    * </ul>
    */
   public boolean initialize (  ) {
      this.addressBean =getActiveAddressBean( );
      urlPath = this.addressBean.getPredicateValue( "url" );
      try {
		url = new URL(urlPath);
	} catch (MalformedURLException e) {
		logger.error("Loading the http wrapper failed : "+e.getMessage(),e);
		return false;
	}
      inputRate = this.addressBean.getPredicateValue( "rate" );
      if ( inputRate == null || inputRate.trim( ).length( ) == 0 ) rate = DEFAULT_RATE;
      else
         rate = Integer.parseInt( inputRate );
      logger.debug( "AXISWirelessCameraWrapper is now running @" + rate + " Rate." );
      return true;
   }
   
   public void run ( ) {
	   ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(1024*20);
	   byte[] buffer = new byte[16*1024];
	   BufferedInputStream content;
      while ( isActive( ) ) {
    	 try {
            Thread.sleep( rate );
			httpURLConnection = (HttpURLConnection) url.openConnection();
		    httpURLConnection.connect();
			if ( httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED ) continue;
		    content = new BufferedInputStream(httpURLConnection.getInputStream(),4096);
		    arrayOutputStream.reset();
		    int readIndex = -1;
		    while ( (readIndex= content.read(buffer))!=-1)
		    	arrayOutputStream.write(buffer, 0, readIndex);
		    postStreamElement(  arrayOutputStream.toByteArray());
         } catch ( InterruptedException e ) {
            logger.error( e.getMessage( ) , e );
         }catch (IOException e) {
		    logger.error( e.getMessage( ) , e );
	     }
      }
   }
   public String getWrapperName() {
    return "Http Receiver";
}
   
   public void dispose (  ) {
      threadCounter--;
   }
   
   public  DataField[] getOutputFormat ( ) {
      return outputStructure;
   }
   
}
