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
* File: src/gsn/tests/TestSafeStorageGetDbUrl.java
*
* @author Sofiane Sarni
* @author Ali Salehi
*
*/

package gsn.tests;

import static org.junit.Assert.assertEquals;
import gsn.acquisition2.SafeStorageDB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

/*
* Unit test for creation of SafeStorage DB Path
* used in SafeStorageDB and CSV checkpoints
* */
public class TestSafeStorageGetDbUrl {

    //by default db files are expected to be in GSN's default directory
    private static final String DEFAULT_SAFESTORAGE_PATH = ".";

    private static final String SAFESTORAGE_DB_NAME = "storage25000";

    File safestorage_properties_file = null;

    @BeforeClass
    public static void init() {

    }

    @Test
    public void testSafeStoragePath() {

        String dbURL = null;
        String expected_dbURL = null;
        String safestorage_path = null;

        delete_properties_file("safestorage.junit.tmp"); // delete if already exists

        // Case 1:
        // non-existent properties file is specified,
        // expected behavior: use default path value
        //

        dbURL = SafeStorageDB.getDBUrl("non_existent_file", SAFESTORAGE_DB_NAME);
        expected_dbURL = "jdbc:h2:" + DEFAULT_SAFESTORAGE_PATH + "/" + SAFESTORAGE_DB_NAME + ".h2";

        System.out.println(dbURL + " <=> " + expected_dbURL);

        assertEquals(dbURL, expected_dbURL);

        // Case 2:
        // a properties file and a property are specified,
        // expected behavior: use the path specified in the property
        //

        safestorage_path = "safestorage_dir";
        create_properties_file("safestorage.junit.tmp", safestorage_path);

        dbURL = SafeStorageDB.getDBUrl("safestorage.junit.tmp", SAFESTORAGE_DB_NAME);
        expected_dbURL = "jdbc:h2:" + safestorage_path + "/" + SAFESTORAGE_DB_NAME + ".h2";

        System.out.println(dbURL + " <=> " + expected_dbURL);

        assertEquals(dbURL, expected_dbURL);

        // Case 3:
        // a file is given but with an empty property,
        // expected behavior: use the default path for safe storage

        safestorage_path = "";
        create_properties_file("safestorage.junit.tmp", safestorage_path);

        dbURL = SafeStorageDB.getDBUrl("safestorage.junit.tmp", SAFESTORAGE_DB_NAME);
        expected_dbURL = "jdbc:h2:" + DEFAULT_SAFESTORAGE_PATH + "/" + SAFESTORAGE_DB_NAME + ".h2";

        System.out.println(dbURL + " <=> " + expected_dbURL);

        assertEquals(dbURL, expected_dbURL);

        delete_properties_file("safestorage.junit.tmp"); // delete on exit

    }

    private void delete_properties_file(String f) {
        File file = new File(f);
        file.delete();
    }

    private void create_properties_file(String f, String property) {

        Properties properties = new Properties();

        properties.setProperty("path", property);

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            properties.store(out, property);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    

}

