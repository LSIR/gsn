package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.ParamParser;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class ChartVirtualSensorPermasense extends AbstractVirtualSensor {
   
   private final transient Logger logger = Logger.getLogger( this.getClass() );
   
   private final HashMap < String , ChartInfoBackLog > input_stream_name_to_ChartInfo_map = 
	   new HashMap < String , ChartInfoBackLog >( );
   
   private ChartGenerator generator;
   
   private static Object mutex = new Object();
   
   public boolean initialize ( ) {
      TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
      ChartInfoBackLog chartInfo = new ChartInfoBackLog( );
      chartInfo.setInputStreamName( params.get( "input-stream" ) );      
      chartInfo.setPlotTitle( params.get( "title" ) );
      chartInfo.setHeight( ParamParser.getInteger( params.get( "height" ) , 480 ) );
      chartInfo.setWidth( ParamParser.getInteger( params.get( "width" ) , 640 ) );
      chartInfo.setVerticalAxisTitle( params.get( "vertical-axis" ) );
      chartInfo.setHistorySize( ParamParser.getInteger( params.get( "history-size" ) , 10 ) );
      chartInfo.setTimeStreamName( params.get( "time-stream-name" ).toLowerCase() );
      chartInfo.setLogVerticalAxis( params.get( "log-vertical-axis" ) == null ? false : true );
      input_stream_name_to_ChartInfo_map.put( chartInfo.getInputStreamName( ) , chartInfo );
      chartInfo.initialize( );
      generator = new ChartGenerator();
      generator.setPriority(Thread.MIN_PRIORITY);
      generator.start();
      return true;
   }
   
	class ChartGenerator extends Thread
	{
		private boolean stopped = false;
		private boolean triggered = false;
		private Object event = new Object();
		
		public void run() {
			
			try { 
				Thread.sleep(60000); 
			} catch (InterruptedException e) { 
				return;
			}

			while (!stopped) {
				try {
					synchronized (event) {
						while (!triggered) {
							event.wait();
						}
						triggered = false;
					}
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					break;
				}			

				synchronized (mutex) {
					String [ ] fieldNames = input_stream_name_to_ChartInfo_map.keySet( ).toArray( new String [ ] {} );
					Byte [ ] fieldTypes = new Byte [ fieldNames.length ];
					Serializable [ ] charts = new Serializable [ fieldNames.length ];
					for ( int i = 0 ; i < fieldTypes.length ; i++ ) {
						/**
						 * We set the type of the output stream element to Types.Binary because
						 * we are producing images.
						 */
						fieldTypes[ i ] = DataTypes.BINARY;
					}
					/**
					 * Creating an stream element with the specified fieldnames, fieldtypes
					 * and using the current time as the timestamp of the stream element.
					 */

					/**
					 * In here our stream element's relation contains just one row of data and
					 * it's filled using the binary data which contains the plots. Note that
					 * this virtual sensor plots one diagram for each InputStreamName. Also
					 * Note that, each InputStreamName can have one or more variables inside
					 * it's stream elements's relation thus having one plot for several
					 * variables.
					 */

					for ( int i = 0 ; i < fieldNames.length ; i++ ) {
						ChartInfoBackLog chart = input_stream_name_to_ChartInfo_map.get( fieldNames[ i ] );
						charts[ i ] = chart.writePlot( ).toByteArray( );
					}
					StreamElement output = new StreamElement( fieldNames , fieldTypes , charts , System.currentTimeMillis( ) );

					/**
					 * Informing container about existance of a stream element.
					 */
					dataProduced( output );
				}
			}
		}
		
		public void trigger() {
			synchronized (event) {
				triggered = true;
				event.notify();
			}
		}
		
		public void interrupt() {
			stopped = true;
			super.interrupt();
		}
	}

   
   public void dataAvailable ( String inputStreamName , StreamElement streamElement ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( "data received under the name *" ).append( inputStreamName ).append( "* to the ChartVS." ).toString( ) );
      /**
       * Finding the appropriate ChartInfo object for this input stream.
       */
      ChartInfoBackLog chartInfo = input_stream_name_to_ChartInfo_map.get( inputStreamName );
      /**
       * If there is not chartInfo configured for this input stream, the virtual
       * sensor doesn't produce any values. Note that if this virtual sensor is
       * intended to produce output other than plots (e.g., if output of this
       * virtual sensor also container integers), then one might comment the
       * following line.
       */
      
      if ( chartInfo == null ) {
         logger.warn( "ChartVS drops the input because there is no chart specification defined for the specific input." );
         return;
      }
      /**
       * Sending the data to the chartInfo.
       */
      chartInfo.addData( streamElement );
      
      generator.trigger();
   }
   
   public void dispose ( ) {
	   generator.interrupt();
	   try {
		   generator.join();
	   } catch (InterruptedException e) { /* ignore */ }
   }
   
}

/**
 * This class represents a chart. The class is initialized using a String with a
 * predefined syntax. The class acts as a proxy between the Virtual Sensor and
 * the JFreeChart library which is used for plotting diagrams.
 */

class ChartInfoBackLog {
   
   private  final transient Logger   logger          = Logger.getLogger( this.getClass() );
   
   private String                          plotTitle;
   
   private int                             width;
   
   private int                             height;
   
   private int                             historySize;
   
   //private String                          type;
   
   private String                          rowData;
   
   private String                          inputStreamName;

   private String                          timeStreamName = null;

   private TimeSeriesCollection            dataCollectionForTheChart;
   
   private HashMap < String , TimeSeries > dataForTheChart = new HashMap < String , TimeSeries >( );
   
   private ByteArrayOutputStream           byteArrayOutputStream;
   
   private JFreeChart                      chart;
   
   private boolean                         changed         = true;
   
   private boolean                         ready           = false;
   
   private String                          verticalAxisTitle;
   
   private boolean                         logVerticalAxis = false;
   
   public ChartInfoBackLog ( ) {
      byteArrayOutputStream = new ByteArrayOutputStream( 64 * 1024 ); // Grows
      // as
      // needed
      byteArrayOutputStream.reset( );
      dataCollectionForTheChart = new TimeSeriesCollection( );
      rowData = "";
   }
   
   public void setWidth ( int width ) {
      if ( !ready ) this.width = width;
   }
   
   public void setHeight ( int height ) {
      if ( !ready ) this.height = height;
   }
   
   public void setHistorySize ( int history ) {
      if ( !ready ) historySize = history;
   }
   
   public void setVerticalAxisTitle ( String title ) {
      if ( !ready ) verticalAxisTitle = title;
   }
   
   /*
   public void setType ( String type ) {
      if ( !ready ) this.type = type;
   }
   */
   
   public void setPlotTitle ( String plotTitle ) {
      if ( !ready ) this.plotTitle = plotTitle;
   }
   
   public void setInputStreamName ( String inputStreamName ) {
      if ( !ready ) this.inputStreamName = inputStreamName;
   }
   
   public void setTimeStreamName ( String timeStreamName ) {
	      if ( !ready ) this.timeStreamName = timeStreamName;
   }   
   
   public void setLogVerticalAxis(boolean b) {
	   if ( !ready ) this.logVerticalAxis = b; 
   }
   
   public void initialize ( ) {
      if ( !ready ) {
         chart = ChartFactory.createTimeSeriesChart( plotTitle , "Time" , verticalAxisTitle , dataCollectionForTheChart , true , true , false );
         chart.setBorderVisible( true );
         XYPlot plot = chart.getXYPlot();
         XYDotRenderer renderer = new XYDotRenderer();
         renderer.setDotHeight(1);
         renderer.setDotWidth(1);
         if (logVerticalAxis) {
        	 plot.setRangeAxis(new LogarithmicAxis(verticalAxisTitle));
         }
         plot.setRenderer(renderer);
         ready = true;
         if ( logger.isDebugEnabled( ) ) logger.debug( "The Chart Virtual Sensor is ready." );
      }
   }
   
   /**
    * This method adds the specified stream elements to the timeSeries of the
    * appropriate plot.
    * 
    * @param streamElement
    */
   public void addData ( StreamElement streamElement ) {
	   for ( int i = 0 ; i < streamElement.getFieldNames( ).length ; i++ ) {
		   if (timeStreamName != null && timeStreamName.equalsIgnoreCase(streamElement.getFieldNames( )[ i ])) continue;
		   if (streamElement.getData()[i] == null) continue;
		   TimeSeries timeSeries = dataForTheChart.get( streamElement.getFieldNames( )[ i ].toLowerCase() );
		   if ( timeSeries == null ) {
			   dataForTheChart.put( streamElement.getFieldNames( )[ i ].toLowerCase() , timeSeries = new TimeSeries( streamElement.getFieldNames( )[ i ].toLowerCase() , org.jfree.data.time.FixedMillisecond.class ) );
			   timeSeries.setMaximumItemCount( historySize );
			   dataCollectionForTheChart.addSeries( timeSeries );
		   }
		   try {
			   Double tmp = Double.parseDouble( streamElement.getData( )[ i ].toString( ) );
			   if (this.logVerticalAxis && tmp <= 0.0) {
				   logger.debug("Values less than or equal to zero not allowed with logarithmic axis");
				   continue;
			   }
			   if (timeStreamName != null)
				   timeSeries.addOrUpdate( new FixedMillisecond( ((Long) streamElement.getData(timeStreamName)).longValue() ) , tmp );
			   else
				   timeSeries.addOrUpdate( new FixedMillisecond( streamElement.getTimeStamp( ) ) , tmp );
		   } catch ( SeriesException e ) {
			   logger.warn( e.getMessage( ) , e );
		   }

	   }
	   changed = true;
   }
   
   /**
    * Plots the chart and sends it in the form of ByteArrayOutputStream to
    * outside.
    * 
    * @return Returns the byteArrayOutputStream.
    */
   public ByteArrayOutputStream writePlot ( ) {
      if ( !changed ) return byteArrayOutputStream;
      byteArrayOutputStream.reset( );
      try {
         ChartUtilities.writeChartAsPNG( byteArrayOutputStream , chart , width , height , false , 8 );
         
      } catch ( IOException e ) {
         logger.warn( e.getMessage( ) , e );
      }
      return byteArrayOutputStream;
   }
   
   public boolean equals ( Object obj ) {
      if ( obj == null && !( obj instanceof ChartInfoBackLog ) ) return false;
      return ( obj.hashCode( ) == hashCode( ) );
   }
   
   int cachedHashCode = -1;
   
   public int hashCode ( ) {
      if ( rowData != null && cachedHashCode == -1 ) cachedHashCode = rowData.hashCode( );
      return cachedHashCode;
   }
   
   /**
    * @return Returns the inputStreamName.
    */
   public String getInputStreamName ( ) {
      return inputStreamName;
   }
   
   public String toString ( ) {
      StringBuffer buffer = new StringBuffer( );
      try {
         if ( plotTitle != null ) buffer.append( "Plot-Title : " ).append( plotTitle ).append( "\n" );
         if ( inputStreamName != null ) {
            buffer.append( "Input-Stream Name : " ).append( inputStreamName ).append( "\n" );
         }
         buffer.append( "Width : " ).append( width ).append( "\n" );
         buffer.append( "Height : " ).append( height ).append( "\n" );
         buffer.append( "History-size : " ).append( historySize ).append( "\n" );
      } catch ( Exception e ) {
         buffer.insert( 0 , "ERROR : Till now the ChartVirtualSensor instance could understand the followings : \n" );
      }
      return buffer.toString( );
   }
}
