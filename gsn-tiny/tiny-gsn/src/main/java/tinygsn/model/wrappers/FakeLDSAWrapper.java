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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/wrappers/AndroidRotationVectorWrapper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers;


import java.util.ArrayList;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.WrapperConfig;
import tinygsn.services.WrapperService;

public class FakeLDSAWrapper extends AbstractWrapper {

	public FakeLDSAWrapper(WrapperConfig wc) {
		super(wc);
	}
	public FakeLDSAWrapper() {
	}

	private static final String[] FIELD_NAMES = new String[] { "ldsa" };
	private static final Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE };
	private static final String[] FIELD_DESCRIPTION = new String[] { "ldsa"};
	private static final String[] FIELD_TYPES_STRING = new String[] { "double"};
	
	public final Class<? extends WrapperService> getSERVICE(){ return FakeLDSAService.class;}

	@Override
	public void runOnce() {

		updateWrapperInfo();
	}

	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[] {});
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}
	
	public static class FakeLDSAService extends WrapperService{

		public FakeLDSAService() {
			super("FakeLDSAService");

		}
	}

}