package gsn.beans;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

/**
 * This class holds the files changed in the virtual-sensor directory and
 * adds/removes the virtual sensors based on the changes.
 *
 */
public final class Modifications {

	private final ArrayList < VSFile > addVirtualSensorConf    = new ArrayList < VSFile >( );

	private final ArrayList < VSFile > removeVirtualSensorConf = new ArrayList < VSFile >( );

	private static transient Logger     logger                  = Logger.getLogger( Modifications.class );

	/**
	 * The list of the virtual sensors, sorted by dependency relations between them,
	 * to be added to the GSN.
	 *
	 * @return Returns the add.
	 */
	public ArrayList < VSFile > getAdd ( ) {
		return addVirtualSensorConf;
	}

	/**
	 * @param add The add to set.
	 */
	public void setAdd ( final Collection < String > add ) {
		addVirtualSensorConf.clear( );
		ArrayList<VSFile> toAdd = new ArrayList<VSFile>();
		loadVirtualSensors( add , toAdd );
	
		List<VSFile> nodesByDFSSearch  = null; //= graph.getNodesByDFSSearch();
		for (VSFile config : nodesByDFSSearch) {
			int indexOf = toAdd.indexOf(config);
			if(indexOf != -1){
				addVirtualSensorConf.add(toAdd.get(indexOf));
			}
		}
	}

	/**
	 * Note that the list parameter should be an empty arrayList;
	 *
	 * @param fileNames
	 * @param list
	 */
	private void loadVirtualSensors ( Collection < String > fileNames , ArrayList < VSFile > list ) {
		if ( fileNames == null || list == null ) throw new RuntimeException( "Null pointer Exception (" + ( fileNames == null ) + "),(" + ( list == null ) + ")" );
		IBindingFactory bfact;
		IUnmarshallingContext uctx;
		try {
			bfact = BindingDirectory.getFactory( VSFile.class );
			uctx = bfact.createUnmarshallingContext( );
		} catch ( JiBXException e1 ) {
			logger.fatal( e1.getMessage( ) , e1 );
			return;
		}
		VSFile configuration;
		for ( String file : fileNames ) {
			try {
				configuration = ( VSFile ) uctx.unmarshalDocument( new FileInputStream( file ) , null );
				configuration.setFileName( file );
				if ( !configuration.validate( ) ) {
					logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( file ).append( " failed because of one or more problems in configuration file." )
							.toString( ) );
					logger.error( new StringBuilder( ).append( "Please check the file and try again" ).toString( ) );
					continue ;
				}

				list.add( configuration );
			} catch ( JiBXException e ) {
				logger.error( e.getMessage( ) , e );
				logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( file ).append(
				" failed because there is syntax error in the configuration file. Please check the configuration file and try again." ).toString( ) );
			} catch ( FileNotFoundException e ) {
				logger.error( e.getMessage( ) , e );
				logger.error( new StringBuilder( ).append( "Adding the virtual sensor specified in " ).append( file ).append( " failed because the configuratio of I/O problems." ).toString( ) );
			}
		}
	}

	/**
	 * The list of the virtual sensors which should be removed.
	 *
	 * @return Returns the remove.
	 */
	public ArrayList < VSFile > getRemove ( ) {
		return removeVirtualSensorConf;
	}

		/**
	 * Construct a new Modifications object.
	 *
	 * @param add : The list of the virtual sensor descriptor files added.
	 * @param remove : The list of the virtual sensor descriptor files removed.
	 */
	public Modifications ( final Collection < String > add , final Collection < String > remove ) {
//		setRemove( remove );
		setAdd( add );
	}


}
