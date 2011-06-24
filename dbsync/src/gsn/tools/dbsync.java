package gsn.tools;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.sql.*;
import java.util.*;

public class dbsync {

    private static transient Logger logger = Logger.getLogger(dbsync.class);

    private static String user_master = null;
    private static String password_master = null;
    private static String driver_master = null;
    private static String url_master = null;

    private static String user_slave = null;
    private static String password_slave = null;
    private static String driver_slave = null;
    private static String url_slave = null;

    private static List<String> tablesList = null;
    private static List<Long> latestTimestampsMaster = null;
    private static List<Long> latestTimestampsSlave = null;
    private static List<Long> numberOfNewerTuples = null;
    private static String runningMode = "";

    static List<String> loadTablesList(String tablesListFileName) {
        List<String> tablesList = new Vector<String>();
        File aFile = new File(tablesListFileName);

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(aFile));
        } catch (FileNotFoundException e) {
            logger.error("Couldn't load list of tables to sync");
            logger.error(e.getMessage(), e);
            return null;
        }

        String aLine = null;

        try {
            while ((aLine = reader.readLine()) != null) {
                tablesList.add(aLine);
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Couldn't load list of tables to sync");
            logger.error(e.getMessage(), e);
            return null;
        }

        return tablesList;
    }

    private static void showList(List<String> tablesList) {
        Iterator iter = tablesList.iterator();
        int i = 0;
        while (iter.hasNext()) {
            logger.info(++i + ") " + iter.next().toString());
        }
    }

    private static List<Long> getLatestTimestamps(Connection conn) {
        List<Long> latestTimestamps = new ArrayList<Long>();
        Iterator iter = tablesList.iterator();

        Statement st = null;
        ResultSet rs = null;

        while (iter.hasNext()) {
            String table = (String) iter.next();
            try {
                st = conn.createStatement();
                rs = st.executeQuery("SELECT max(timed) FROM " + table);

                if (rs.next()) {
                    long ts = rs.getLong(1);
                    latestTimestamps.add(ts);
                }

            } catch (SQLException e) {
                logger.error("Couldn't create list of latest timestamps");
                logger.error(e.getMessage(), e);
                return null;
            }
        }

        try {
            rs.close();
            st.close();
            conn.close();
        } catch (SQLException e) {
            logger.error("Couldn't create list of latest timestamps");
            logger.error(e.getMessage(), e);
            return null;
        }

        return latestTimestamps;
    }

    private static List<Long> getNumberOfNewerTuples(Connection conn) {
        List<Long> numberOfNewerTuples = new ArrayList<Long>();

        Statement st = null;
        ResultSet rs = null;

        for (int i = 0; i < tablesList.size(); i++) {
            String table = tablesList.get(i);
            try {
                st = conn.createStatement();
                rs = st.executeQuery("SELECT count(*) FROM " + table + " WHERE timed>" + latestTimestampsSlave.get(i));

                if (rs.next()) {
                    long ts = rs.getLong(1);
                    numberOfNewerTuples.add(ts);
                }

            } catch (SQLException e) {
                logger.error("Couldn't create list of newer tuples");
                logger.error(e.getMessage(), e);
                return null;
            }
        }

        try {
            rs.close();
            st.close();
            conn.close();
        } catch (SQLException e) {
            logger.error("Couldn't create list of newer tuples");
            logger.error(e.getMessage(), e);
            return null;
        }

        return numberOfNewerTuples;
    }

    public static void listTimestamps() {
        logger.info("Table\tMaster\tSlave\tCount");
        for (int i = 0; i < tablesList.size(); i++) {
            logger.info(tablesList.get(i) + "\t" + latestTimestampsMaster.get(i) + "\t" + latestTimestampsSlave.get(i) + "\t" + numberOfNewerTuples.get(i));
        }
    }

    public static void syncTable(String table, long latestTS) {

        logger.info("Synchronizing table [ " + table + " ]...");

        String selectQuery = "select * from " + table + " where timed > " + latestTS;

        Connection connMaster = null;
        Connection connSlave = null;
        logger.info(selectQuery);

        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        PreparedStatement pstmt = null;

        try {
            connMaster = getMasterConnection();
            connSlave = getSlaveConnection();
            rs = connMaster.createStatement().executeQuery(selectQuery);
            rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            StringBuilder sbCols = new StringBuilder();
            StringBuilder sbValues = new StringBuilder();
            logger.info("cols: " + colCount);
            for (int i = 2; i <= colCount; i++) {        // skip pk
                sbCols.append(rsmd.getColumnName(i));
                sbValues.append("? ");
                if (i < colCount) {
                    sbCols.append(", ");
                    sbValues.append(", ");
                }
            }
            logger.info(sbCols.toString());

            long rowCount = 0;
            String insertStatement = "INSERT INTO " + table + " (" + sbCols + ") VALUES(" + sbValues + ")";
            pstmt = connSlave.prepareStatement(insertStatement);
            logger.info(insertStatement);
            while (rs.next()) {
                StringBuilder aRow = new StringBuilder();
                for (int i = 2; i <= colCount; i++) {   // skip pk
                    Object o = rs.getObject(i);
                    pstmt.setObject(i - 1, o);          // columns are shifted because of skipped pk
                    //aRow.append(o).append(", ");
                }
                //logger.info(pstmt.toString());
                //logger.info(aRow.toString());
                pstmt.executeUpdate();
                rowCount++;
            }
            logger.info("Inserted " + rowCount + " rows.");


        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        try {
            pstmt.close();
            rs.close();
            connMaster.close();
            connSlave.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } catch (NullPointerException e) {
            logger.error(e.getMessage(), e);
        }


        logger.info("Table [ " + table + " ] done.");

    }

    public static void main(java.lang.String[] args) {

        PropertyConfigurator.configure("conf/log4j_dbsync.properties");

        if (args.length>0) {
            runningMode = args[0];
            logger.info("Running mode :" + runningMode);
        }

        Properties masterdb = new Properties();
        Properties slavedb = new Properties();

        try {
            masterdb.load(new FileInputStream("conf/masterdb.properties"));
            slavedb.load(new FileInputStream("conf/slavedb.properties"));
        } catch (IOException e) {
            logger.error("Couldn't load properties file");
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }

        user_master = masterdb.getProperty("user");
        password_master = masterdb.getProperty("password");
        driver_master = masterdb.getProperty("driver");
        url_master = masterdb.getProperty("url");

        user_slave = slavedb.getProperty("user");
        password_slave = slavedb.getProperty("password");
        driver_slave = slavedb.getProperty("driver");
        url_slave = slavedb.getProperty("url");

        logger.info("Synchronizing tables (Master => Slave)");
        logger.info("Master: [user: " + user_master + " , password: " + password_master + " , driver: " + driver_master + ", url: " + url_master + " ]");
        logger.info("Slave: [user: " + user_slave + " , password: " + password_slave + " , driver: " + driver_slave + ", url: " + url_slave + " ]");

        // tables to sync
        tablesList = loadTablesList("conf/tables");
        if (tablesList == null)
            System.exit(-1);
        logger.info("Tables to Sync:");
        showList(tablesList);

        try {
            Class.forName(driver_master);
            Class.forName(driver_slave);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }

        Connection conn = null;
        try {
            conn = getMasterConnection();
            latestTimestampsMaster = getLatestTimestamps(conn);
            conn = getSlaveConnection();
            latestTimestampsSlave = getLatestTimestamps(conn);
            conn = getMasterConnection();
            numberOfNewerTuples = getNumberOfNewerTuples(conn);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }

        listTimestamps();

        if (runningMode.compareTo("test") == 0) {
            System.exit(0);
        }

        // Synchronizing all tables
        for (int i = 0; i < tablesList.size(); i++) {
            syncTable(tablesList.get(i), latestTimestampsSlave.get(i));
        }


    }

    private static Connection getMasterConnection() throws Exception {
        return DriverManager.getConnection(url_master, user_master, password_master);
    }

    private static Connection getSlaveConnection() throws Exception {
        return DriverManager.getConnection(url_slave, user_slave, password_slave);
    }


}
