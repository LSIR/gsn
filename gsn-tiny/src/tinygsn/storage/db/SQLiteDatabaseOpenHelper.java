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

import java.io.Serializable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

public class SQLiteDatabaseOpenHelper extends SQLiteOpenHelper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4345330326921532L;

	public SQLiteDatabaseOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/tinygsn/"+ name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String createQuery = "CREATE TABLE vsList (_id integer primary key autoincrement,"
				+ "running, vsname, vstype, "
				+ "notify_field, notify_condition, notify_value, notify_action, notify_contact, save_to_db"
				+ ");";
		db.execSQL(createQuery);
		
		createQuery = "CREATE TABLE sourcesList (_id integer primary key autoincrement,"
				+ "vsname, sswindowsize, ssstep, sstimebased, sssamplingrate, ssaggregator, wrappername, "
				+ ");";
		db.execSQL(createQuery);

		createQuery = "CREATE TABLE SUBSCRIPTION_ROW (_id integer primary key,"
				+ "SERVER text, VSNAME text, DATA text, READ text"
				+ ");";
		db.execSQL(createQuery);
	
		createQuery = "CREATE TABLE SAMPLIG_RATE (_id integer primary key, time bigint,"
				+ "samplingrate integer, vsname text"
				+ ");";
		db.execSQL(createQuery);
		
		createQuery = "CREATE TABLE WifiFrequency (_id integer primary key,"
				+ "frequency integer, mac text"
				+ ");";
		db.execSQL(createQuery);
		createQuery = "CREATE TABLE Samples (_id integer primary key, time bigint, "
				+ "sample integer,"
				+ "reason integer"
				+ ");";
		db.execSQL(createQuery);
	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("Drop table vsList");
		db.execSQL("Drop table sourcesList");
		db.execSQL("Drop table SUBSCRIPTION_ROW");
		db.execSQL("Drop table SAMPLIG_RATE");
		onCreate(db);
	}

}
