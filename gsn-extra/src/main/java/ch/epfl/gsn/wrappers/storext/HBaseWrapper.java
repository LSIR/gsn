/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/wrappers/storext/HBaseWrapper.java
*
* @author Ivo Dimitrov
*
*/

package ch.epfl.gsn.wrappers.storext;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.wrappers.AbstractWrapper;

import org.slf4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class HBaseWrapper extends AbstractWrapper {

    private HBaseConnector hbase;

    private static final int            DEFAULT_SAMPLING_RATE       = 1000;

    private int                         samplingRate                = DEFAULT_SAMPLING_RATE;

    private final transient Logger logger                      = LoggerFactory.getLogger(HBaseWrapper.class);

    private static int                  threadCounter               = 0;

    private DataField[] outputStructure; /* = new DataField [ ] { new DataField( "HEAP" , "varchar" , "Heap memory usage." ) ,
            new DataField( "NON_HEAP" , "varchar" , "Nonheap memory usage." ) , new DataField( "PENDING_FINALIZATION_COUNT" , "varchar" , "The number of objects with pending finalization." ) }; */

    private static final String       FIELD_NAME_HEAP                       = "HEAP";

    private static final String       FIELD_NAME_NON_HEAP                   = "NON_HEAP";

    private static final String       FIELD_NAME_PENDING_FINALIZATION_COUNT = "PENDING_FINALIZATION_COUNT";

    private static final String [ ]   FIELD_NAMES                           = new String [ ] { FIELD_NAME_HEAP , FIELD_NAME_NON_HEAP , FIELD_NAME_PENDING_FINALIZATION_COUNT };


    private String table_name;
    private String rowKey;
    private String fieldNames;
    //private String column_Family;


    public boolean initialize() {
        logger.info("Initializing HBaseWrapper Class");
        String javaVersion = System.getProperty("java.version");
        if(!javaVersion.startsWith("1.6")){
            logger.error("Error in initializing HBaseWrapper because of incompatible jdk version: " + javaVersion + " (should be 1.6.x)");
            return false;
        }
        // load the xml predicates
        AddressBean addressBean = getActiveAddressBean();
        // get each interesting value
        table_name = addressBean.getPredicateValue("table-name");
        rowKey = addressBean.getPredicateValue("rowKey");
        fieldNames = addressBean.getPredicateValue("fields");
        ArrayList<DataField> output = new ArrayList<DataField>();
        String[] fields = fieldNames.split(",");  // seperate the names of the individual fields
        for (String f : fields) {
             output.add(new DataField( f , "varchar" , "output field" ));
        }
        outputStructure = output.toArray(outputStructure);
        /*fieldNames = addressBean.getPredicateValue("fields");
        System.out.println("~~~~~~~~~~~~~ "+ fieldNames);
        column_Family = addressBean.getPredicateValue("column-Family");
        System.out.println("~~~~~~~~~~~~~ "+ column_Family);  */
        // connect to the HBase
        hbase = new HBaseConnector();

        return true;
    }

    public void run(){
        HashMap<String, ArrayList<Pair>> results;
        while(isActive()){
            try{
                Thread.sleep(samplingRate);
            }catch (InterruptedException e){
                logger.error(e.getMessage(), e);
            }

            if (rowKey.compareTo("") != 0 ) { // if a row is selected
                results = null;
            } else {
                results = hbase.getAllRecords(table_name);
            }

            Set<Map.Entry<String, ArrayList<Pair>>> entrySet = results.entrySet();
            for (Entry entry : entrySet) {
                ArrayList<Pair> columns = (ArrayList<Pair>) entry.getValue();  // the columns associated with a particular row
                Byte[] dataFieldTypes = new Byte [columns.size()];
                for (Byte b: dataFieldTypes) b = DataTypes.VARCHAR;  // assume everything is String
                Serializable[ ] dataFields  = new Serializable[columns.size()];
                int i = 0;
                for(Pair p : columns) {
                    dataFields[i++] = p.getFieldValue();
                }
                StreamElement streamElement = new StreamElement( FIELD_NAMES , dataFieldTypes , dataFields, System.currentTimeMillis( ) );
                postStreamElement( streamElement );
            }

        }
    }

    public void dispose() {
        threadCounter--;
    }

    public String getWrapperName() {
        return "HBaseWrapper";
    }

    public DataField[] getOutputFormat() {
        return outputStructure;
    }

}

