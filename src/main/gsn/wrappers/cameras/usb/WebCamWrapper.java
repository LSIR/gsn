package gsn.wrappers.cameras.usb;

/**
 * Before using this class make sure you have the <code>jmf.jar</code>
 * [form http://java.sun.com/products/java-media/jmf/index.jsp] in your classpath.
 * Once you have the above jar file in the classpath (e.g., by putting it in the lib directory of GSN,
 * you need to remove the <code>excludes="gsn/wrappers/cameras/usb/WebCamWrapper.java"</code> from the
 * build.xml file so that GSN will compile this file.
 */

// For more resources see :
// http://www.geocities.com/marcoschmidt.geo/java-image-coding.html

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

import java.awt.Graphics2D;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.NotRealizedError;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.control.FormatControl;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.CaptureDevice;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.util.BufferToImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class WebCamWrapper implements Wrapper , ControllerListener {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	public static final String            PICTURE_KEY                  = "PICTURE";

	private final ByteArrayOutputStream   baos                         = new ByteArrayOutputStream( 16 * 1024 );

	private Buffer                        buff                         = new Buffer( );

	private PushBufferStream              camStream;                                                             // Global

	private JPEGImageEncoder              codec                        = JPEGCodec.createJPEGEncoder( baos );

	private BufferToImage                 converter;                                                             // Global

	private JPanel                        lable                        = new JPanel( );

	private ImageWrapper                  reading;                                                               // Contains

	private Object                        stateLock                    = new Object( );

	private int                           height;

	private int                           width;

	private JFrame                        mainFrame;

	private DataSource                    ds                           = null;

	private Processor                     deviceProc                   = null;

	private PushBufferDataSource          source                       = null;

	private static final transient Logger logger                       = Logger.getLogger( WebCamWrapper.class );

	/**
	 * for debugging purposes.
	 */
	private static int                    threadCounter                = 0;

	public static final String            DEFAULT_GSN_LOG4J_PROPERTIES = "conf/log4j.properties";

	private transient final static  DataField [] dataField = new DataField[] {new DataField( PICTURE_KEY , "binary:image/jpeg" )};
	// -----------------------------------START----------------------------------------
	// DEVICE NAME FOR LINUX CAM: "v4l:OV518 USB Camera:0"
	private boolean isLiveViewEnabled = false;

	public WebCamWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		String liveView = conf.getParameters().getPredicateValueWithException( "live-view" );
		String deviceName=conf.getParameters().getPredicateValueWithException("device");
		try {
			isLiveViewEnabled = Boolean.parseBoolean( liveView );
		} catch ( Exception e ) {
			throw new RuntimeException( "The >liveView< parameter is not a valid boolean (WebCamWrapper)" , e );
		}
		webcamInitialization( isLiveViewEnabled ,deviceName);
	}

	private boolean webcamInitialization ( boolean liveView,String deviceName ) {
		CaptureDeviceInfo device = CaptureDeviceManager.getDevice( deviceName );
		if (device==null) {
			logger.error("Device doesn't exist: "+deviceName);
			return false;
		}
		MediaLocator loc = device.getLocator( );
		try {
			ds = Manager.createDataSource( loc );
		} catch ( NoDataSourceException e ) {
			// Did you load the module ?
			// Did you set the Env ?
			logger.error( "Unable to create dataSource[Did you set the environment + load the module]" );
			logger.error( e.getMessage( ) , e );
			return false;
		} catch ( IOException e ) {
			logger.error( "IO Error creating dataSource" );
			logger.error( e.getMessage( ) , e );
			return false;
		}
		if ( !( ds instanceof CaptureDevice ) ) {
			logger.error( "DataSource not a CaptureDevice" );
			return false;
		}

		FormatControl [ ] fmtControls = ( ( CaptureDevice ) ds ).getFormatControls( );

		if ( fmtControls == null || fmtControls.length == 0 ) {
			logger.error( "No FormatControl available" );
			return false;
		}

		Format setFormat = null;
		YUVFormat userFormat = null;
		for ( Format format : device.getFormats( ) )
			if ( format instanceof YUVFormat ) userFormat = ( YUVFormat ) format;

		this.width = userFormat.getSize( ).width;
		this.height = userFormat.getSize( ).height;

		for ( int i = 0 ; i < fmtControls.length ; i++ ) {
			if ( fmtControls[ i ] == null ) continue;
			if ( ( setFormat = fmtControls[ i ].setFormat( userFormat ) ) != null ) {
				break;
			}
		}
		if ( setFormat == null ) {
			logger.error( "Failed to set device to specified mode" );
			return false;
		}

		try {
			ds.connect( );
		} catch ( IOException ioe ) {
			logger.error( "Unable to connect to DataSource" );
			return false;
		}
		logger.debug( "Data source created and format set" );
		try {
			deviceProc = Manager.createProcessor( ds );
		} catch ( IOException ioe ) {
			logger.error( "Unable to get Processor for device: " + ioe.getMessage( ) );
			return false;
		} catch ( NoProcessorException npe ) {
			logger.error( "Unable to get Processor for device: " + npe.getMessage( ) );
			return false;
		}
		/*
		 * In order to use the controller we have to put it in the realized state.
		 * We do this by calling the realize method, but this will return
		 * immediately so we must register a listener (this class) to be notified
		 * TIMED the controller is ready. The class containing this code must
		 * implement the ControllerListener interface.
		 */

		deviceProc.addControllerListener( this );
		deviceProc.realize( );

		while ( deviceProc.getState( ) != Controller.Realized ) {
			synchronized ( stateLock ) {
				try {
					stateLock.wait( );
				} catch ( InterruptedException ie ) {
					logger.error( "Device failed to get to realized state" );
					return false;
				}
			} 		
		}

		deviceProc.start( );
		System.out.println( "Just before streaming." );
		logger.info( "Before Streaming" );
		try {
			source = ( PushBufferDataSource ) deviceProc.getDataOutput( );
		} catch ( NotRealizedError nre ) {
			/* Should never happen */
			logger.error( "Internal error: processor not realized" );
			return false;
		}

		PushBufferStream [ ] streams = source.getStreams( );

		for ( int i = 0 ; i < streams.length ; i++ )
			if ( streams[ i ].getFormat( ) instanceof RGBFormat ) {
				camStream = streams[ i ];
				RGBFormat rgbf = ( RGBFormat ) streams[ i ].getFormat( );
				converter = new BufferToImage( rgbf );
			} else if ( streams[ i ].getFormat( ) instanceof YUVFormat ) {
				camStream = streams[ i ];
				YUVFormat yuvf = ( YUVFormat ) streams[ i ].getFormat( );
				converter = new BufferToImage( yuvf );
			}
		if ( liveView ) {
			mainFrame = new JFrame( "Webcam's current observations [GSN Project]." );
			mainFrame.getContentPane( ).add( lable );
			mainFrame.setSize( getWidth( ) + 10 , getHeight( ) + 10 );
			mainFrame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
			mainFrame.setResizable( false );
			mainFrame.setVisible( true );
		}
		return true;
	}

	public void controllerUpdate ( ControllerEvent ce ) {
		if ( ce instanceof RealizeCompleteEvent ) {
			logger.info( "Realize transition completed" );
			synchronized ( stateLock ) {
				stateLock.notifyAll( );
			}
		}
	}

	private int getHeight ( ) {
		return height;
	}

	private int getWidth ( ) {
		return width;
	}

	private Image getImage ( ) {
		try {
			camStream.read( buff );
		} catch ( Exception ioe ) {
			logger.error( "Unable to capture frame from camera" );
			logger.error( ioe.getMessage( ) , ioe );
			return null;
		}
		return converter.createImage( buff );
	}

	public  DataField[] getOutputFormat ( ) {
		return dataField;
	}

	private StreamElement getStreamElement ( ) {
		StreamElement streamElement = null;
		try {
			baos.reset( );
			if ( reading != null ) {
				codec.encode( reading.getBufferedImage( ) );
				streamElement = StreamElement.from(this).set( PICTURE_KEY ,baos.toByteArray( )).setTime(System.currentTimeMillis( ) );
			}
		} catch ( Exception e ) {
			logger.error( e.getMessage( ) , e );
		}
		return streamElement;
	}

	public void start(){
		reading = new ImageWrapper( getImage( ) );
		int i=0;
		while ( isActive ) {
			if (isLiveViewEnabled) {
				Graphics2D graphics2D = ( Graphics2D ) lable.getGraphics( );
				graphics2D.drawImage( reading.getImage( ) , 0 , 0 , null );
			}
			//if ( lastPicture++ % INTERVAL != 0 ) continue;
			// System.out.println("CALLED"+i++);
			reading.setImage( getImage( ) );
			dataChannel.write( getStreamElement( ) );
		}
	}

	private boolean isActive=true;

	public void dispose ( ) {

	}
	public void stop() {
		isActive = false;
		source.disconnect( );
		deviceProc.stop( );
		deviceProc.deallocate( );
		deviceProc.close( );
		ds.disconnect( );
		if ( mainFrame != null ) mainFrame.dispose( );
	}

	public static void main ( String [ ] args ) {
		PropertyConfigurator.configure( DEFAULT_GSN_LOG4J_PROPERTIES );
		if (args.length==0)
			printDeviceList();
		else if (args.length==1) {
			System.out.println("Trying to connect to capturing device: "+args[0]);
			WebCamWrapper test = new WebCamWrapper(null, null );
			boolean initialization = test.webcamInitialization( true ,args[0]); 
			if ( initialization ) test.start( );
			else
				System.out.println( "Start Failed." );
		}else {
			System.err.println("You can call this method either without any argument to get the list of the devices and with the arguments to capture.");
		}
	}
	public static void printDeviceList() {
		System.out.println("List of capturing devices: ");
		Vector devices = CaptureDeviceManager.getDeviceList(null);
		Enumeration list = devices.elements();
		while (list.hasMoreElements()) {
			CaptureDeviceInfo deviceInfo =(CaptureDeviceInfo) list.nextElement();
			String name = deviceInfo.getName();

			Format[] fmts = deviceInfo.getFormats();
			System.out.println("NAME: " +name);
			for (int i = 0; i < fmts.length; i++) {
				System.out.println("\t"+fmts[i]);
			}
		}
	}





}
