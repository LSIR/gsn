package gsn.http;

import gsn.Mappings;
import gsn.beans.VSensorConfig;
import org.postgis.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class PostGisTest {

    private static List<String> sensors;
    private static List<Point> coordinates;


    public static Connection connect(String url, String dbuser, String dbpass) throws SQLException,
            ClassNotFoundException {
        Connection conn;
        Class.forName("org.postgis.DriverWrapper");
        conn = DriverManager.getConnection(url, dbuser, dbpass);
        return conn;
    }

    public static String getListOfSensors() {
        StringBuilder s = new StringBuilder();
        Iterator iter = Mappings.getAllVSensorConfigs();
        while (iter.hasNext()) {
            VSensorConfig sensorConfig = (VSensorConfig) iter.next();
            Double longitude = sensorConfig.getLongitude();
            Double latitude = sensorConfig.getLatitude();
            Double altitude = sensorConfig.getAltitude();
            String sensor = sensorConfig.getName();

            if ((latitude != null) && (longitude != null) && (altitude != null)) {
                Point point = new Point(latitude, longitude, altitude);
                coordinates.add(point);
                sensors.add(sensor);
                s.append(sensor)
                        .append(" => ")
                        .append(longitude)
                        .append(" : ")
                        .append(latitude)
                        .append("\n");
            }
        }
        return s.toString();
    }

    /*
   * Returns list of sensors currently loaded in the system (dummy)
   * */

    public static String getListOfSensorsDummy() {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            Double longitude = 1.0 * i;
            Double latitude = 100.0 - i;
            Double altitude = 1.0 * i * i;
            String sensor = "Sensor_" + i;

            if ((latitude != null) && (longitude != null) && (altitude != null)) {
                Point point = new Point(latitude, longitude, altitude);
                coordinates.add(point);
                sensors.add(sensor);
                s.append(sensor)
                        .append(" => ")
                        .append(longitude)
                        .append(", ")
                        .append(latitude)
                        .append(", ")
                        .append(altitude)
                        .append(" == ")
                        .append(point).append("\n");
            }
        }
        return s.toString();
    }

    public static void buildIndex() {
        sensors = new Vector<String>();
        coordinates = new Vector<Point>();
    }

    public static Properties loadProperties() {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("conf/spatial.properties"));
        }
        catch (IOException e) {

        }
        return p;
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {

        buildIndex();
        Properties properties = loadProperties();

        System.out.println(getListOfSensorsDummy());
        String dburl = properties.getProperty("dburl");
        String dbuser = properties.getProperty("dbuser");
        String dbpass = properties.getProperty("dbpass");
        Connection conn = connect(dburl, dbuser, dbpass);

        //((org.postgresql.PGConnection) conn).addDataType("geometry", "org.postgis.PGgeometry");
        //((org.postgresql.PGConnection) conn).addDataType("box3d", "org.postgis.PGbox3d");

        String st_create_table = "DROP INDEX IF EXISTS gist_sensors;"
                + " DROP TABLE IF EXISTS sensors;"
                + " CREATE TABLE sensors ( \"name\" character(255) NOT NULL, \"location\" geometry NOT NULL );"
                + " CREATE INDEX gist_sensors ON sensors USING GIST ( location ); ";

        System.out.println(st_create_table);

        PreparedStatement prepareStatement = conn.prepareStatement(st_create_table);
        prepareStatement.execute();
        prepareStatement.close();

        for (int i = 0; i < coordinates.size(); i++) {
            String insert = "insert into sensors values ( '" + sensors.get(i) + "', ST_MakePoint(" + coordinates.get(i).getX() + " , " + coordinates.get(i).getY() + " , " + coordinates.get(i).getZ() + ") );";
            PreparedStatement ps = conn.prepareStatement(insert);
            ps.execute();
            ps.close();
            System.out.println(insert);
        }

        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("select location, name from sensors");
        while (r.next()) {
            PGgeometry geom = (PGgeometry) r.getObject(1);
            String name = r.getString(2);
            System.out.println("Geometry " + geom.toString() + " : " + name);
        }
        s.close();
        conn.close();
    }

}


