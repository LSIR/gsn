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
* File: gsn-tiny/src/tinygsn/gui/android/TinyGSN.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android;

import org.kroz.activerecord.ActiveRecordBase;
import org.kroz.activerecord.ActiveRecordException;
import org.kroz.activerecord.Database;
import org.kroz.activerecord.DatabaseBuilder;

import tinygsn.beans.StaticData;
import tinygsn.gui.android.utils.SensorRow;
import tinygsn.utils.Const;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class TinyGSN extends Application {

	public ActiveRecordBase _db;

	public TinyGSN() {

	}

	@Override
	public void onCreate() {
		super.onCreate();

//		// --------- Prepare mDatabase connection ----------
//		DatabaseBuilder builder = new DatabaseBuilder(Const.DATABASE_NAME);
//		builder.addClass(SubscriptionRow.class);
//		Database.setBuilder(builder);
//		try {
//			_db = ActiveRecordBase.open(this, Const.DATABASE_NAME,
//					Const.DATABASE_VERSION);
//			Log.v("TinyGSN", "Open ok");
//		}
//		catch (ActiveRecordException e) {
//			e.printStackTrace();
//		}
		
		StaticData.globalContext = getApplicationContext();

		Log.v("TinyGSN", "TinyGSN is called!");

	}
}
