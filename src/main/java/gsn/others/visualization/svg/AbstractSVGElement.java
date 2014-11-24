/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: src/gsn/others/visualization/svg/AbstractSVGElement.java
*
* @author Ali Salehi
*
*/

package gsn.others.visualization.svg;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public abstract class AbstractSVGElement implements SVGElement {
   
   private String  link;
   
   private Map     attributes;
   
   private TreeSet listOfRels = null;
   
   private Object  internalNode;
   
   public String getLink ( ) {
      return link;
   }
   
   public void setLink ( String link ) {
      this.link = link;
   }
   
   public void printLinkStartElement ( StringBuilder input ) {
      if ( link != null ) input.append( SVGUtils.hyperLinkingAddress( link ) );
      
   }
   
   public void printLinkEndElement ( StringBuilder input ) {
      if ( link != null ) input.append( "\n</a>" );
   }
   
   private boolean enable = true;
   
   public boolean isEnable ( ) {
      return enable;
   }
   
   public void setEnable ( boolean enable ) {
      this.enable = enable;
   }
   
   class UniqueCompare implements Comparator {
      
      public int compare ( Object o1 , Object o2 ) {
         return o1.toString( ).compareTo( o2.toString( ) );
      }
   }
   
   public void addRelationship ( SVGEdge rel ) {
      listOfRels.add( rel );
   }
   
   public SVGEdge [ ] getRelationships ( ) {
      int size = listOfRels.size( );
      return ( SVGEdge [ ] ) this.listOfRels.toArray( new SVGEdge [ size ] );
   }
   
   public List getRelatedEntities ( ) {
      int size = listOfRels.size( );
      SVGEdge [ ] a_listOfRels = ( SVGEdge [ ] ) this.listOfRels.toArray( new SVGEdge [ size ] );
      LinkedList listOfRelatedEntities = new LinkedList( );
      for ( int i = 0 ; i < a_listOfRels.length ; i++ ) {
         SVGEdge rel = a_listOfRels[ i ];
         if ( rel.sourceEntity != this && rel.destinationEntity != this ) { throw new RuntimeException( "Problem, we have a relationship and we are not the source or the dest" ); }
         if ( rel.sourceEntity != this ) {
            listOfRelatedEntities.add( rel.sourceEntity );
         }
         if ( rel.destinationEntity != this ) {
            listOfRelatedEntities.add( rel.destinationEntity );
         }
      }
      return listOfRelatedEntities;
   }
   
   /**
    * An algorithm may require a place to store information. Use this structure
    * for that purpose.
    */
   public void setAttributeInLayout ( Object attribute , Object value ) {
      attributes.put( attribute , value );
   }
   
   /**
    * An algorithm may require a place to store information. Use this structure
    * for that purpose.
    */
   public Object getAttributeInLayout ( Object attribute ) {
      return attributes.get( attribute );
   }
   
   // all objects are equal
   public int compareTo ( Object arg0 ) {
      return 0;
   }
   
   public Object getLayoutInformation ( ) {
      return internalNode;
   }
   
   public void setLayoutInformation ( Object internalEntity ) {
      this.internalNode = internalEntity;
   }
}
