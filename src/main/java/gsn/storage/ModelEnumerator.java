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
* File: src/gsn/storage/ModelEnumerator.java
*
* @author Julien Eberle
*
*/

package gsn.storage;


import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.models.AbstractModel;

public class ModelEnumerator implements DataEnumeratorIF {
	
	private static final transient Logger logger = Logger.getLogger(ModelEnumerator.class);
	
	private StreamElement[] results;
	private int ptr;
	
	public ModelEnumerator(String query, AbstractModel model) {
		String where = SQLUtils.extractWhereClause(query).toLowerCase();
		String[] ex = where.split(" and ");
		DataField[] df = new DataField[ex.length];
		Serializable[] sr = new Serializable[ex.length];
		int i = 0;
		for(String s : ex){
			String[] v = s.split(" = ");
			df[i] = new DataField(v[0],"double"); // !!! _HARDCODED, only supports double
			sr[i] = Double.parseDouble(v[1]);
			i ++;
		}
		ptr = -1;
		results = model.query(new StreamElement(df,sr));
	}

	@Override
	public boolean hasMoreElements() {
		return results != null && ptr < results.length-1;
	}

	@Override
	public StreamElement nextElement() throws RuntimeException {
		if (hasMoreElements()){
			ptr = ptr + 1;
			return results[ptr];
		}else{
			return null;
		}
	}

	@Override
	public void close() {
		
	}

}
