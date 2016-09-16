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
* File: src/ch/epfl/gsn/wrappers/MemoryMonitoringWrapper.java
*
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package ch.epfl.gsn.wrappers;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.utils.ParamParser;
import ch.epfl.gsn.wrappers.AbstractWrapper;
import ch.epfl.gsn.wrappers.MemoryMonitoringWrapper;

import org.slf4j.Logger;

public class MemoryMonitoringWrapper extends AbstractWrapper {
   
   private static final int          DEFAULT_SAMPLING_RATE                 = 1000;
   
   private int                       samplingRate                          = DEFAULT_SAMPLING_RATE;
   
   private final transient Logger    logger                                = LoggerFactory.getLogger( MemoryMonitoringWrapper.class );
   
   private static int                threadCounter                         = 0;
   
   private transient DataField [ ]   outputStructureCache                  = new DataField [ ] { new DataField( FIELD_NAME_HEAP , "bigint" , "Heap memory usage." ) ,
         new DataField( FIELD_NAME_NON_HEAP , "bigint" , "Nonheap memory usage." ) , new DataField( FIELD_NAME_PENDING_FINALIZATION_COUNT , "int" , "The number of objects with pending finalization." ) };
   
   private static final String       FIELD_NAME_HEAP                       = "HEAP";
   
   private static final String       FIELD_NAME_NON_HEAP                   = "NON_HEAP";
   
   private static final String       FIELD_NAME_PENDING_FINALIZATION_COUNT = "PENDING_FINALIZATION_COUNT";
   
   private static final String [ ]   FIELD_NAMES                           = new String [ ] { FIELD_NAME_HEAP , FIELD_NAME_NON_HEAP , FIELD_NAME_PENDING_FINALIZATION_COUNT };
   
   private static final MemoryMXBean mbean                                 = ManagementFactory.getMemoryMXBean( );
   
   public boolean initialize ( ) {
      AddressBean addressBean = getActiveAddressBean( );
      if ( addressBean.getPredicateValue( "sampling-rate" ) != null ) {
         samplingRate = ParamParser.getInteger( addressBean.getPredicateValue( "sampling-rate" ) , DEFAULT_SAMPLING_RATE );
         if ( samplingRate <= 0 ) {
            logger.warn( "The specified >sampling-rate< parameter for the >MemoryMonitoringWrapper< should be a positive number.\nGSN uses the default rate (" + DEFAULT_SAMPLING_RATE + "ms )." );
            samplingRate = DEFAULT_SAMPLING_RATE;
         }
      }
      return true;
   }
   
   public void run ( ) {
      while ( isActive( ) ) {
         try {
            Thread.sleep( samplingRate );
         } catch ( InterruptedException e ) {
            logger.error( e.getMessage( ) , e );
         }
         long heapMemoryUsage = mbean.getHeapMemoryUsage( ).getUsed( );
         long nonHeapMemoryUsage = mbean.getNonHeapMemoryUsage( ).getUsed( );
         int pendingFinalizationCount = mbean.getObjectPendingFinalizationCount( );
         
         StreamElement streamElement = new StreamElement( FIELD_NAMES , new Byte [ ] { DataTypes.BIGINT , DataTypes.BIGINT , DataTypes.INTEGER } , new Serializable [ ] { heapMemoryUsage ,
               nonHeapMemoryUsage , pendingFinalizationCount } , System.currentTimeMillis( ) );
         postStreamElement( streamElement );
      }
   }
   
   public void dispose ( ) {
      threadCounter--;
   }
   
   /**
    * The output fields exported by this virtual sensor.
    * 
    * @return The strutcture of the output.
    */
   
   public final DataField [ ] getOutputFormat ( ) {
      return outputStructureCache;
   }
   
   public String getWrapperName ( ) {
      return "System memory consumption usage";
   }
}
