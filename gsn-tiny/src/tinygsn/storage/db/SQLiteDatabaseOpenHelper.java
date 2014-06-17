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
* File: gsn-tiny/src/tinygsn/storage/db/SQLiteDatabaseOpenHelper.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.storage.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteDatabaseOpenHelper extends SQLiteOpenHelper {

	public SQLiteDatabaseOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String createQuery = "CREATE TABLE vsList (_id integer primary key autoincrement,"
				+ "running, vsname, vstype, "
				+ "sswindowsize, ssstep, sssamplingrate, ssaggregator, wrappername, "
				+ "notify_field, notify_condition, notify_value, notify_action, notify_contact, save_to_db"
				+ ");";
		db.execSQL(createQuery);

		createQuery = "CREATE TABLE SUBSCRIPTION_ROW (_id integer primary key,"
				+ "SERVER text, VSNAME text, DATA text, READ text"
				+ ");";
		db.execSQL(createQuery);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("Drop table vsList");
		db.execSQL("Drop table SUBSCRIPTION_ROW");
		onCreate(db);
	}

}
