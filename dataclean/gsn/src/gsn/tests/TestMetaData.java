package gsn.tests;

import gsn.storage.StorageManager;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.DriverManager;

import org.apache.log4j.Logger;


public class TestMetaData {

    
    private static final transient Logger logger = Logger.getLogger(TestMetaData.class);

    private static final String TABLE_NAME_PARAM = "table_name";
    private static final String META_DATA_TABLE_NAME = "allmetadata";

    /*
    * executes an SQL query
    * and returns the result as string
    * */
    static public String executeQuery(String query) {

        String s="";
        logger.warn("metadata query: "+query);

        //
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("ERROR IN EXECUTING, query: " + query);
            logger.error(e.getMessage(), e);
            return "";
        }
        //



        try {
            //Connection conn = StorageManager.getInstance().getConnection();
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/csv",
        "sa", "");

            Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(query);
            while (rs.next()){
                 s= rs.getString(1);
            }
            logger.warn("result: "+s);
        } catch (SQLException e) {
            logger.error("ERROR IN EXECUTING, query: " + query);
            logger.error(e.getMessage(), e);
            return "";
        } finally {
            //TODO:close connections
            //rs.close();
            //stmt.close();
            //conn.close();
            //
        }

        return s;
    }

    public void syncMetaDataTable() {
       // update table META_DATA_TABLE_NAME
    }

    static String getLocation(int sensor_id, long timed, String table_name) {

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct location from ")
                .append(META_DATA_TABLE_NAME)
                .append(" where sensorid=")
                .append(sensor_id)
                .append(" and ")
                .append(timed)
                .append(" >= fromdate and ")
                .append(timed)
                .append(" < todate");

         return executeQuery(sb.toString());

        /*
        set @timed=1256753982105;
set @sensor_id=2007;
SELECT distinct location
FROM allmetadata
WHERE allmetadata.sensorid=@sensor_id and
      @timed>=fromdate and
  @timed<todate
        * */
/*
set @timed=1256753982105;
set @sensor_id=2007;
SELECT distinct @timed, allmetadata.location
FROM allmetadata
WHERE allmetadata.sensorid=@sensor_id and
      @timed>=allmetadata.fromdate and
	  @timed<allmetadata.todate

	  return allmetadata.location
	  */

    }

    static String getMeasuredProperty(int sensor_id, long timed, String table_name, String parameter_name) {

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct measured_property from ")
                .append(META_DATA_TABLE_NAME)
                .append(" where sensorid=")
                .append(sensor_id)
                .append(" and ")
                .append(timed)
                .append(" >= fromdate and ")
                .append(timed)
                .append(" < todate")
                .append(" and parameter_name=\"")
                .append(parameter_name)
                .append("\" and table_name=\"")
                .append(table_name)
                .append("\"");

         return executeQuery(sb.toString());

        /*
  set @db_param='PAYLOAD_SIBADCDIFF0';
  set @timed=1256753982105;
  set @sensor_id=2028;
  set @table_name='matterhorn_dozeradccomdiff_3933';
  SELECT distinct measured_property
  FROM allmetadata
  WHERE sensorid=@sensor_id and
        @timed>=fromdate and
        @timed<todate and
        parameter_name=@db_param and
        table_name=@table_name; */
    }

    public static void main(String[] args) {
        System.out.println(getLocation(2007,1256753982105L,""));
        System.out.println(getMeasuredProperty(2028,1256753982105L,"matterhorn_dozeradccomdiff_3933","PAYLOAD_SIBADCDIFF0"));

        

    }
}
/*
        set @timed=1256753982105;
set @sensor_id=2007;
SELECT distinct location
FROM allmetadata
WHERE allmetadata.sensorid=@sensor_id and
      @timed>=fromdate and
  @timed<todate
        * */
/*
set @timed=1256753982105;
set @sensor_id=2007;
SELECT distinct @timed, allmetadata.location
FROM allmetadata
WHERE allmetadata.sensorid=@sensor_id and
      @timed>=allmetadata.fromdate and
	  @timed<allmetadata.todate

	  return allmetadata.location
	  */