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
* File: src/gsn/wrappers/storext/HBaseConnector.java
*
* @author Ivo Dimitrov
*
*/

package gsn.wrappers.storext;

/**
 * Created with IntelliJ IDEA.
 * User: ivo
 * Date: 4/16/13
 * Time: 9:15 PM
 * To change this template use File | Settings | File Templates.
 */
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.ScannerCallable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.filter.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HBaseConnector {

    private static final long ROW_NUMBER = 10;  // number of records read each time
    private static Configuration confHBase = null;
    private long checkpoint;  // the timestamp until which we have returned results
    private ResultScanner ss;
    private boolean hasNext;   // shows if there are more values to be retrieved
    private HashMap<String, ArrayList<Pair>> results;  // key = row, value = the field and its value
    /**
     * Initialization
     */
    static {
        try {
        confHBase = HBaseConfiguration.create();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public HBaseConnector() {
        results = new HashMap<String, ArrayList<Pair>>();
    }

    /**
     * Create a table
     */
    public void createTable(String tableName, String[] familys)
            throws Exception {
        HBaseAdmin admin = new HBaseAdmin(confHBase);
        if (admin.tableExists(tableName)) {
            System.out.println("table already exists!");
        } else {
            HTableDescriptor tableDesc = new HTableDescriptor(tableName);
            for (int i = 0; i < familys.length; i++) {
                tableDesc.addFamily(new HColumnDescriptor(familys[i]));
            }
            admin.createTable(tableDesc);
            System.out.println("create table " + tableName + " ok.");
        }
    }

    /**
     * Delete a table
     */
    public void deleteTable(String tableName) throws Exception {
        try {
            HBaseAdmin admin = new HBaseAdmin(confHBase);
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
            System.out.println("delete table " + tableName + " ok.");
        } catch (MasterNotRunningException e) {
            e.printStackTrace();
        } catch (ZooKeeperConnectionException e) {
            e.printStackTrace();
        }
    }

    public boolean tableExists(String tablename) throws Exception {
        try {
            HBaseAdmin admin = new HBaseAdmin(confHBase);
            return admin.tableExists(tablename);
        }catch (MasterNotRunningException e) {
                e.printStackTrace();
        } catch (ZooKeeperConnectionException e) {
                e.printStackTrace();
        }
        return false;
    }

    /**
     * Put (or insert) a row
     */
    public void addRecord(String tableName, String rowKey,
                                 String family, String qualifier, String value) throws Exception {
        try {
            HTable table = new HTable(confHBase, tableName);
            Put put = new Put(Bytes.toBytes(rowKey));
            put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes
                    .toBytes(value));
            table.put(put);
           /* System.out.println("insert recored " + rowKey + " to table "
                    + tableName + " ok.");*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete a row
     */
    public void delRecord(String tableName, String rowKey)
            throws IOException {
        HTable table = new HTable(confHBase, tableName);
        List<Delete> list = new ArrayList<Delete>();
        Delete del = new Delete(rowKey.getBytes());
        list.add(del);
        table.delete(list);
        System.out.println("del recored " + rowKey + " ok.");
    }

    /**
     * Get a row
     */
    public ArrayList<Pair> getOneRecord (String tableName, String rowKey) throws IOException{
        ArrayList<Pair> columns = new ArrayList<Pair>();
        HTable table = new HTable(confHBase, tableName);
        Get get = new Get(rowKey.getBytes());
        Result rs = table.get(get);
        for(KeyValue kv : rs.raw()){
            columns.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));
        }
        return columns;
    }
    /**
     * Scan (or list) a table
     */
    public HashMap<String, ArrayList<Pair>> getAllRecords (String tableName) {
        try{
            HTable table = new HTable(confHBase, tableName);
            Scan s = new Scan();

            ResultScanner ss = table.getScanner(s);
            results.clear();  // clear the map before continuing

            for(Result r:ss){
                ArrayList<Pair> dataFields = new ArrayList<Pair>();
                KeyValue kv2 = null;
                for(KeyValue kv : r.raw()){        // for each different key that you find
                    kv2 = kv;
                    dataFields.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));  // add a new column associated with this row
                }
                results.put(new String(kv2.getRow()), dataFields);
            }
            return results;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

   public HashMap<String, ArrayList<Pair>> getCondRecords (String tableName, String cond, int N) {
        try{
            String[] parts = cond.split("=");
            HTable table = new HTable(confHBase, tableName);
        //System.out.println("Table = "+tableName+" Col= "+parts[0]+" value="+parts[1]);
            SingleColumnValueFilter filterA = new SingleColumnValueFilter(Bytes.toBytes("columnFamily"), Bytes.toBytes(parts[0]), CompareFilter.CompareOp.EQUAL, Bytes.toBytes(parts[1]));
            Filter filter1 = new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,new BinaryComparator(Bytes.toBytes("row-22")));
            FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
            filterList.addFilter(filter1);
            Scan s = new Scan();
            s.setFilter(filterList);
            ResultScanner ss = table.getScanner(s);
            results.clear();  // clear the map before continuing
            int counter = N;
            for(Result r:ss){
	//System.out.println("Outer for");
                ArrayList<Pair> dataFields = new ArrayList<Pair>();
                KeyValue kv2 = null;
                for(KeyValue kv : r.raw()){        // for each different key that you find
        //System.out.println("Inner for");
                    kv2 = kv;
                    dataFields.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));  // add a new column associated with this row
                }
                results.put(new String(kv2.getRow()), dataFields);
                counter--;
                if (counter == 0) break; // if we gathered the window that we want
            }
            return results;
        } catch (IOException e){
System.out.println("Problem");
            e.printStackTrace();
            return null;
        }
    }



    public HashMap<String, ArrayList<Pair>> getNRecords (String tableName, int N) {
        try{
            HTable table = new HTable(confHBase, tableName);
            Scan s = new Scan();

            ResultScanner ss = table.getScanner(s);
            results.clear();  // clear the map before continuing
            int counter = N;
            for(Result r:ss){
                ArrayList<Pair> dataFields = new ArrayList<Pair>();
                KeyValue kv2 = null;
                for(KeyValue kv : r.raw()){        // for each different key that you find
                    kv2 = kv;
                    dataFields.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));  // add a new column associated with this row
                }
                results.put(new String(kv2.getRow()), dataFields);
                counter--;
                if (counter == 0) break; // if we gathered the window that we want
            }
            return results;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public HashMap<String, ArrayList<Pair>> getIntervalRecords (String tableName, String start, String end) {
        try{
            HTable table = new HTable(confHBase, tableName);
            Scan s = new Scan();
            s.setStartRow(start.getBytes());
            s.setStopRow(end.getBytes());

            ResultScanner ss = table.getScanner(s);
            results.clear();  // clear the map before continuing

            for(Result r:ss){
                ArrayList<Pair> dataFields = new ArrayList<Pair>();
                KeyValue kv2 = null;
                for(KeyValue kv : r.raw()){        // for each different key that you find
                    kv2 = kv;
                    dataFields.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));  // add a new column associated with this row
                }
                results.put(new String(kv2.getRow()), dataFields);
            }
            return results;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }


    ////////////////////// Code added on 06.05.2013
    public HashMap<String, ArrayList<Pair>> getNextRecords (String tableName, boolean start) {  // records are retrieved based on timestamp
        try{
            Scan scan;
            long iterations = ROW_NUMBER;

            HTable table = new HTable(confHBase, tableName);
            if (start == true) {  // a new scan is initiated
                scan = new Scan();
                ss = table.getScanner(scan);
            }

            results.clear();  // clear the map before continuing
            Result r;
            setHasNext(false);  // assume that there is no next
            for (r = ss.next(); r != null; r = ss.next()) {
                System.out.println("#################");
                ArrayList<Pair> dataFields = new ArrayList<Pair>();
                KeyValue kv2 = null;
                for(KeyValue kv : r.raw()){        // for each different key that you find
                    kv2 = kv;
                    dataFields.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));  // add a new column associated with this row
                 /**/   System.out.print(new String(kv.getRow()) + " ");
                    System.out.print(new String(kv.getFamily()) + ":");
                    System.out.print(new String(kv.getQualifier()) + " ");
                    System.out.print(kv.getTimestamp() + " ");
                    System.out.println(new String(kv.getValue()));   /**/
                }
                results.put(new String(kv2.getRow()), dataFields);
                iterations--;
                if (iterations == 0) break;
            }
            setHasNext(r != null);  // set if there are more to be retrieved
            return results;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public HashMap<String, ArrayList<Pair>> getRecordsAfterTimestamp (String tableName, boolean start, long time) {  // records are retrieved based on timestamp
        try{
            Scan scan;
            long iterations = ROW_NUMBER;

            HTable table = new HTable(confHBase, tableName);
            if (start == true) {  // a new scan is initiated
                scan = new Scan();
                checkpoint = time;
                scan.setTimeRange(checkpoint, Long.MAX_VALUE);  // go till the end
                ss = table.getScanner(scan);
            }

            results.clear();  // clear the map before continuing
            Result r;
            setHasNext(false);  // assume that there is no next
            for (r = ss.next(); r != null; r = ss.next()) {
                System.out.println("#################");
                ArrayList<Pair> dataFields = new ArrayList<Pair>();
                KeyValue kv2 = null;
                for(KeyValue kv : r.raw()){        // for each different key that you find
                    kv2 = kv;
                    dataFields.add(new Pair(new String(kv.getQualifier()), new String(kv.getValue())));  // add a new column associated with this row
                 /**/   System.out.print(new String(kv.getRow()) + " ");
                    System.out.print(new String(kv.getFamily()) + ":");
                    System.out.print(new String(kv.getQualifier()) + " ");
                    System.out.print(kv.getTimestamp() + " ");
                    System.out.println(new String(kv.getValue()));   /**/
                }
                results.put(new String(kv2.getRow()), dataFields);
                iterations--;
                if (iterations == 0) break;
            }
            setHasNext(r != null);  // set if there are more to be retrieved
            return results;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }


    public boolean hasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
    //////////////////////



    // For Testing purposes
    /*
    public static void main(String[] agrs) {
        try {
            String tablename = "scores";
            String[] familys = { "grade", "course" };
            HBaseConnector.creatTable(tablename, familys);

            // add record zkb
            HBaseConnector.addRecord(tablename, "zkb", "grade", "", "5");
            HBaseConnector.addRecord(tablename, "zkb", "course", "", "90");
            HBaseConnector.addRecord(tablename, "zkb", "course", "math", "97");
            HBaseConnector.addRecord(tablename, "zkb", "course", "art", "87");
            // add record baoniu
            HBaseConnector.addRecord(tablename, "baoniu", "grade", "", "4");
            HBaseConnector.addRecord(tablename, "baoniu", "course", "math", "89");

            System.out.println("===========get one record========");
            HBaseConnector.getOneRecord(tablename, "zkb");

            System.out.println("===========show all record========");
            HBaseConnector.getAllRecord(tablename);

            System.out.println("===========del one record========");
            HBaseConnector.delRecord(tablename, "baoniu");
            HBaseConnector.getAllRecord(tablename);

            System.out.println("===========show all record========");
            HBaseConnector.getAllRecord(tablename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } */
}

