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
* File: src/gsn/wrappers/ieee1451/TedsReader.java
*
* @author Ali Salehi
*
*/

package gsn.wrappers.ieee1451;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.thoughtworks.xstream.XStream;

public class TedsReader {
   
   
   public static void main ( String [ ] args ) {
      try {
         TEDS teds = readTedsFromXMLFile(new File("micaONE.xml") );
         System.out.println( teds.toHtmlString( ) );
         
      } catch ( Exception e ) {
         e.printStackTrace( );
      }
   }
   
   public static TEDS readTedsFromXMLFile (  File fileName ) {
      XStream xs = new XStream( );
      try {
         TEDS teds = new TEDS( ( Object [ ][ ][ ] ) xs.fromXML( new FileInputStream(  fileName ) ) );
         return teds;
      } catch ( FileNotFoundException e ) {
         e.printStackTrace( );
      }
      return null;
   }
   
   public static TEDS readTedsFromBinaryFile ( String TARGET_DIR,String fileName ) {
      FileInputStream fos;
      try {
         fos = new FileInputStream( new File( TARGET_DIR + fileName ) );
         ObjectInputStream os = new ObjectInputStream( fos );
         Object obj = os.readObject( );
         os.close( );
         return ( new TEDS( ( Object [ ][ ][ ] ) obj ) );
      } catch ( FileNotFoundException e ) {
         e.printStackTrace( );
      } catch ( IOException e ) {
         e.printStackTrace( );
      } catch ( ClassNotFoundException e ) {
         e.printStackTrace( );
      }
      return null;
   }
}
