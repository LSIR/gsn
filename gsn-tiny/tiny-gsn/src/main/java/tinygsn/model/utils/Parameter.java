/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/vsensor/NotificationVirtualSensor.java
 *
 * @author Schaer Marc
 */

package tinygsn.model.utils;

import java.util.ArrayList;

public class Parameter {
	private String mName;
	private ArrayList<String> mParameters;
	private String mDefaultParameter = "";
	private ParameterType mType;

	public Parameter(String mName, ParameterType mType) {
		this.mName = mName;
		this.mParameters = new ArrayList<>();
		this.mType = mType;
	}

	public Parameter(String mName, String mDefaultParameter, ParameterType mType) {
		this.mName = mName;
		this.mDefaultParameter = mDefaultParameter;
		this.mType = mType;
	}

	public Parameter(String mName, ArrayList<String> mParameters, ParameterType mType) {
		this.mName = mName;
		if (mParameters == null) {
			this.mParameters = new ArrayList<>();
		} else {
			this.mParameters = mParameters;
		}
		this.mType = mType;
	}

	public String getmName() {
		return mName;
	}

	public void setmName(String mName) {
		this.mName = mName;
	}

	public ArrayList<String> getmParameters() {
		return (ArrayList<String>) mParameters.clone();
	}

	public void setmParameters(ArrayList<String> mParameters) {
		this.mParameters = mParameters;
	}

	public boolean hasParameters() {
		return !this.mParameters.isEmpty();
	}

	public String getmDefaultParameter() {
		return mDefaultParameter;
	}

	public void setmDefaultParameter(String mDefaultParameter) {
		this.mDefaultParameter = mDefaultParameter;
	}

	public ParameterType getmType() {
		return mType;
	}

	public void setmType(ParameterType mType) {
		this.mType = mType;
	}
}
