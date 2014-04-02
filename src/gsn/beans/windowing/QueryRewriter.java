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
* File: src/gsn/beans/windowing/QueryRewriter.java
*
* @author gsn_devs
* @author Mehdi Riahi
*
*/

package gsn.beans.windowing;

import gsn.beans.StreamSource;

public abstract class QueryRewriter {
	protected StreamSource streamSource;
	
	public QueryRewriter(){
		
	}
	
	public QueryRewriter(StreamSource streamSource){
		setStreamSource(streamSource);
	}
	
	public abstract boolean initialize();
	
	public abstract StringBuilder rewrite(String query);
	
	public abstract void dispose();

	public abstract boolean dataAvailable(long timestamp);
	
	public StreamSource getStreamSource() {
		return streamSource;
	}

	public void setStreamSource(StreamSource streamSource) {
		this.streamSource = streamSource;
		streamSource.setQueryRewriter(this);
	}


}
