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
* File: src/gsn/utils/protocols/AbstractHCIQueryWithoutAnswer.java
*
* @author Jerome Rousselot
* @author Ali Salehi
*
*/

package gsn.utils.protocols;

import java.util.Vector;

/**
 * This class provides an empty implementation of the methods
 * getWaitTime, needsAnswer and getAnswers to make it
 * easier to implement queries that don't require an answer.
 */
public abstract class AbstractHCIQueryWithoutAnswer extends AbstractHCIQuery {

   public AbstractHCIQueryWithoutAnswer(String Name, String queryDescription, String[] paramsDescriptions) {
      super(Name, queryDescription, paramsDescriptions);
   }

   // we usually dont expect an answer
   public int getWaitTime ( Vector < Object > params ) {
      // TODO Auto-generated method stub
      return NO_WAIT_TIME;
   }
   
   /* 
    * By default we dont expect an answer. 
    */
   public boolean needsAnswer ( Vector < Object > params ) {
      return false;
   }
   
   /*
    * No answer by default so this is a placeholder method.
    */
   public Object[] getAnswers(byte[] rawAnswer) {
	   return null;
   }
}
