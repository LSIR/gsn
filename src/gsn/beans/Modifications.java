package gsn.beans;

import gsn.Mappings;
import gsn.utils.graph.Graph;
import gsn.utils.graph.Node;
import gsn.utils.graph.NodeNotExistsExeption;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * TODO : Handling the modifications.
 */
public final class Modifications {
   
   private ArrayList < VSensorConfig > addVirtualSensorConf    = new ArrayList < VSensorConfig >( );
   
   private ArrayList < VSensorConfig > removeVirtualSensorConf = new ArrayList < VSensorConfig >( );

   private Graph<VSensorConfig> graph;
   
   private static transient Logger     logger                  = Logger.getLogger( Modifications.class );
   
   /**
    * The list of the virtual sensors, sorted by dependency relations between them,
    * to be added to the GSN.
    * 
    * @return Returns the add.
    */
   public ArrayList < VSensorConfig > getAdd ( ) {
      return addVirtualSensorConf;
   }
   
   /**
    * @param add The add to set.
    */
   public void setAdd ( final Collection < String > add ) {
      addVirtualSensorConf.clear( );
      ArrayList<VSensorConfig> toAdd = new ArrayList<VSensorConfig>();
      loadVirtualSensors( add , toAdd );
      fillGraph(graph, toAdd.iterator());

      List<VSensorConfig> nodesByDFSSearch = graph.getNodesByDFSSearch();
      for (VSensorConfig config : nodesByDFSSearch) {
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
   private void loadVirtualSensors ( Collection < String > fileNames , ArrayList < VSensorConfig > list ) {
      if ( fileNames == null || list == null ) throw new RuntimeException( "Null pointer Exception (" + ( fileNames == null ) + "),(" + ( list == null ) + ")" );
      IBindingFactory bfact;
      IUnmarshallingContext uctx;
      try {
         bfact = BindingDirectory.getFactory( VSensorConfig.class );
         uctx = bfact.createUnmarshallingContext( );
      } catch ( JiBXException e1 ) {
         logger.fatal( e1.getMessage( ) , e1 );
         return;
      }
      VSensorConfig configuration;
      for ( String file : fileNames ) {
         try {
            configuration = ( VSensorConfig ) uctx.unmarshalDocument( new FileInputStream( file ) , null );
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
   public ArrayList < VSensorConfig > getRemove ( ) {
      return removeVirtualSensorConf;
   }
   
   /**
    * @param listOfTheRemovedVirtualSensorsFileName The remove to set.
    */
   public void setRemove ( final Collection < String > listOfTheRemovedVirtualSensorsFileName ) {
      removeVirtualSensorConf.clear( );
      //file has been removed, the virtual sensor and dependent virtual sensors should be deleted
      for (String fileName : listOfTheRemovedVirtualSensorsFileName) {
    	  VSensorConfig vSensorConfig = Mappings.getConfigurationObject(fileName);
    	  if(vSensorConfig != null){
    		  Node<VSensorConfig> node = graph.findNode(vSensorConfig);
    		  if (node != null && removeVirtualSensorConf.contains(vSensorConfig) == false) {
				  //adding to removed list the removed vs and all virtual sensors that depend on it   
				  List<Node<VSensorConfig>> nodesAffectedByRemoval = graph.nodesAffectedByRemoval(node);
				  for (Node<VSensorConfig> toRemoveNode : nodesAffectedByRemoval) {
					  VSensorConfig config = toRemoveNode.getObject();
					  if(removeVirtualSensorConf.contains(config) == false)
						  removeVirtualSensorConf.add(config);
				  }
			  }
    		  try {
				graph.removeNode(vSensorConfig);
			} catch (NodeNotExistsExeption e) {
				// This shouldn't happen 
				logger.error(e.getMessage(), e);
			}
    	  }
	}
   }
   
   /**
    * Construct a new Modifcations object.
    * 
    * @param add : The list of the virtual sensor descriptor files added.
    * @param remove : The list of the virtual sensor descriptor files removed.
    */
   public Modifications ( final Collection < String > add , final Collection < String > remove ) {
	   buildDependencyGraph(); 
	   //the order of the following two methods is important
	   setRemove( remove );
	   setAdd( add );
   }
   
   
   
   public Graph<VSensorConfig> getGraph() {
	  return graph;
   }

private Collection < VSensorConfig > getModifications ( ) {
      // Finding the virtual sensors by nme
      // adapting the output structure.
      // making sure that the registered client's will not leave.
      
      return null;
   }
   
   private void buildDependencyGraph ( ) {
	   graph = new Graph<VSensorConfig>();
	   Iterator<VSensorConfig> allVSensorConfigs = Mappings.getAllVSensorConfigs();
	   fillGraph(graph, allVSensorConfigs);
   }

   private static void fillGraph(Graph<VSensorConfig> graph, Iterator<VSensorConfig> allVSensorConfigs) {
	   HashMap<String, VSensorConfig> vsNameTOVSConfig = new HashMap<String, VSensorConfig>();
	   while(allVSensorConfigs.hasNext()){
		   VSensorConfig config = allVSensorConfigs.next();
		   vsNameTOVSConfig.put(config.getName().toLowerCase().trim(), config);
		   graph.addNode(config);
	   }

outFor:for(VSensorConfig config : vsNameTOVSConfig.values()){ 		
		   Collection<InputStream> inputStreams = config.getInputStreams();
		   for (InputStream stream : inputStreams) {
			   StreamSource[] sources = stream.getSources();
			   for (int i = 0; i < sources.length; i++) {
				   AddressBean[] addressing = sources[i].getAddressing();
				   for (int j = 0; j < addressing.length; j++) {
					   String vsensorName = addressing[i].getPredicateValue("NAME");
					   if (vsensorName != null) {
						   String vsName = vsensorName.toLowerCase().trim();
						VSensorConfig sensorConfig = vsNameTOVSConfig.get(vsName);
						   if(sensorConfig == null)
							   sensorConfig = Mappings.getVSensorConfig(vsName);
						   if(sensorConfig == null){
							   if(logger.isInfoEnabled())
								   logger.info("There is no virtaul sensor with name >" +  vsName + "<");
							   try {
								   graph.removeNode(config);
								   continue outFor;
							   } catch (NodeNotExistsExeption e) {
								   logger.error(e.getMessage(), e);
								   //This shouldn't happen, as we first add all virtual sensors to the graph
							   }
							   continue;
						   }
						   try {
							   if(graph.findNode(sensorConfig) != null)
								   graph.addEdge(config, sensorConfig);
							   else
								   try {
									   graph.removeNode(config);
								   } catch (NodeNotExistsExeption e) {
									   logger.error(e.getMessage(), e);
									   //This shouldn't happen, as we first add all virtual sensors to the graph
								   }
						   } catch (NodeNotExistsExeption e) {
							   logger.error(e.getMessage(), e);
							   //This shouldn't happen, as we first add all virtual sensors to the graph
						   }
					   }
				   }
			   }
		   }
	   }
   }
   
   public static Graph<VSensorConfig> buildDependencyGraphFromIterator(Iterator<VSensorConfig> vsensorIterator){
	   Graph<VSensorConfig> graph = new Graph<VSensorConfig>();
	   fillGraph(graph, vsensorIterator);
	   return graph;
   }
   
}
