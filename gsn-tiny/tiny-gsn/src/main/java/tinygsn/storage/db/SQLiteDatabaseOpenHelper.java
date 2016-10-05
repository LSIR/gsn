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
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
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
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		String createQuery = "CREATE TABLE vsList (_id integer primary key autoincrement,"
				+ "running, vsname, vstype "
				+ ");";
		db.execSQL(createQuery);

		createQuery = "CREATE TABLE sourcesList (_id integer primary key autoincrement,"
				+ "vsname, sswindowsize, ssstep, sstimebased, ssaggregator, wrappername "
				+ ");";
		db.execSQL(createQuery);


		createQuery = "CREATE TABLE publishDestination (_id integer primary key autoincrement,"
				+ "url text, vsname text, clientId text,  clientSecret text, mode integer, lastTime bigint, iterationTime bigint, active int"
				+ ");";
		db.execSQL(createQuery);

		createQuery = "CREATE TABLE subscribeSource (_id integer primary key autoincrement,"
				+ "url text, vsname text, mode integer, lastTime bigint, iterationTime bigint, active int, username text, password text"
				+ ");";
		db.execSQL(createQuery);

		createQuery = "CREATE TABLE wrapperList (_id integer primary key, wrappername text,"
				+ "dcinterval integer, dcduration integer"
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
		createQuery = "CREATE TABLE settings (key text primary key, value text);";
		db.execSQL(createQuery);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {

		//clean the database if any virtual sensor is not completely defined
		String query = "Select * from vsList;";
		Cursor cursor = db.rawQuery(query, new String[] {});
		ArrayList<String> toRemove = new ArrayList<String>();
		while (cursor.moveToNext()){
			String name = cursor.getString(cursor.getColumnIndex("vsname"));

			//check if it has a data table
			query = "SELECT * FROM sqlite_master WHERE type = 'table' AND name = ?;";
			Cursor cursor0 = db.rawQuery(query, new String[] {"vs_"+name});
			if (!cursor0.moveToNext()){
				toRemove.add(name);
			}
			cursor0.close();

			//check if it has at least one stream source
			query = "Select * from sourcesList where vsname = ?;";
			cursor0 = db.rawQuery(query, new String[] {name});
			if (!cursor0.moveToNext()){
				toRemove.add(name);
			}
			cursor0.close();
		}
		cursor.close();
		for (String s: toRemove){
			db.delete("vsList", "vsname = ?", new String[] {s});
			db.delete("sourcesList", "vsname = ?", new String[] {s});
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("Drop table vsList");
		db.execSQL("Drop table sourcesList");
		db.execSQL("Drop table wrapperList");
		db.execSQL("Drop table publishDestination");
		db.execSQL("Drop table Samples");
		db.execSQL("Drop table settings");
		onCreate(db);
	}

}
