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
 * File: gsn-tiny/src/tinygsn/gui/android/TinyGSN.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.gui.android;

import tinygsn.beans.StaticData;

import android.app.Application;
import android.util.Log;

public class TinyGSN extends Application {


	public TinyGSN() {

	}

	@Override
	public void onCreate() {
		super.onCreate();

		StaticData.globalContext = getApplicationContext();

		Log.v("TinyGSN", "TinyGSN is called!");

	}
}
