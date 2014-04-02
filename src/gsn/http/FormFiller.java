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
* File: src/gsn/http/FormFiller.java
*
* @author Ali Salehi
*
*/

package gsn.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

public class FormFiller {
   
   public static void fillFromRequest ( HttpServletRequest req , Object form ) {
      Enumeration < String > params = req.getParameterNames( );
      while ( params.hasMoreElements( ) ) {
         String paramName = params.nextElement( );
         StringBuilder stringBuffer = new StringBuilder( paramName );
         stringBuffer.replace( 0 , 1 , "set" + Character.toUpperCase( paramName.charAt( 0 ) ) );
         Method method;
         try {
            method = form.getClass( ).getMethod( stringBuffer.toString( ) , new Class [ ] { String.class } );
            method.invoke( form , new Object [ ] { req.getParameter( paramName ) } );
         } catch ( SecurityException e ) {
            continue;
         } catch ( NoSuchMethodException e ) {
            continue;
         } catch ( IllegalArgumentException e ) {
            continue;
         } catch ( IllegalAccessException e ) {
            continue;
         } catch ( InvocationTargetException e ) {
            continue;
         }
         
      }
   }
}
