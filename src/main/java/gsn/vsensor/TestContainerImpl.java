/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
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
* File: src/gsn/vsensor/TestContainerImpl.java
*
* @author Ali Salehi
*
*/

package gsn.vsensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import gsn.http.WebConstants;
import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class TestContainerImpl {
   
   @After
   public void clean ( ) {

   }
   
   @Test
   public void gettingListOfVirtualSensors ( ) throws Exception {
      WebConversation wc = new WebConversation( );
      WebRequest request = new GetMethodWebRequest( "http://localhost:22001/gsn" );
      request.setParameter( "REQUEST" , WebConstants.REQUEST_LIST_VIRTUAL_SENSORS + "" );
      WebResponse response = wc.getResponse( request );
      assertTrue( response.getResponseMessage( ).contains( "<gsn>" ) );
      // assertNotNull( response.getHeaderField( Container.RESPONSE ) );
   }
   
   @Test
   public void oneShotQueryExecution ( ) throws Exception {
      WebConversation wc = new WebConversation( );
      WebRequest request = new GetMethodWebRequest( "http://localhost:22001/gsn" );
      request.setHeaderField( "REQUEST" , WebConstants.REQUEST_ONE_SHOT_QUERY + "" );
      request.setHeaderField( "VS_QUERY" , "select * from LocalSystemTime" );
      WebResponse response = wc.getResponse( request );
      assertEquals( response.getHeaderField( WebConstants.RESPONSE_STATUS ) , WebConstants.REQUEST_HANDLED_SUCCESSFULLY );
      assertNull( response.getHeaderField( WebConstants.RESPONSE ) );
   }
   
   @Before
   public void setup ( ) {

   }
   
   @BeforeClass
   public static void init ( ) {

   }
   
   @AfterClass
   public static void cleanAll ( ) {

   }
   
   public static junit.framework.Test suite ( ) {
      return new JUnit4TestAdapter( TestContainerImpl.class );
   }
   
}
