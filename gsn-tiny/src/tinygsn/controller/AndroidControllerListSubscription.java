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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerListSubscription.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import org.kroz.activerecord.ActiveRecordBase;
import org.kroz.activerecord.ActiveRecordException;
import org.kroz.activerecord.Database;
import org.kroz.activerecord.DatabaseBuilder;
import org.kroz.activerecord.utils.Logg;
import tinygsn.beans.StreamElement;
import tinygsn.gui.android.ActivityListSubscription;
import tinygsn.gui.android.utils.SensorRow;
import tinygsn.storage.StorageManager;
import tinygsn.storage.db.SqliteStorageManager;
import tinygsn.utils.Const;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AndroidControllerListSubscription extends AbstractController {

	private ActivityListSubscription view = null;

	private Handler handlerData = null;

	private ArrayList<SensorRow> subsDataList = new ArrayList<SensorRow>();

	private static final String TAG = "AndroidControllerListSubscription";

	private ActiveRecordBase _db;
	private int numLoaded = 0;

	public AndroidControllerListSubscription(
			ActivityListSubscription androidViewer) {
		this.view = androidViewer;

		
	}

    public Handler getHandlerData() {
		return handlerData;
	}

	public void setHandlerData(Handler handlerData) {
		this.handlerData = handlerData;
	}

}