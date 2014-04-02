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
* File: src/gsn/wrappers/MockWrapper.java
*
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class MockWrapper extends AbstractWrapper {
	int threadCounter;
	
	private DataField[] outputFormat =new DataField[] {new DataField("data","int")};

	public boolean initialize() {
		setName("TestWrapperMockObject-Thread" + (++threadCounter));
		return true;
	}

	public void run() {

	}

	public DataField[] getOutputFormat() {
		return outputFormat;
	}

	public boolean publishStreamElement(StreamElement se) {
		return postStreamElement(se);
	}

	public void dispose() {
		threadCounter--;
	}

	public String getWrapperName() {
		return "TestWrapperMock";
	}


	
}
