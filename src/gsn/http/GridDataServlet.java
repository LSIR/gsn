package gsn.http;

import gsn.Main;
import gsn.beans.DataTypes;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;


public class GridDataServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(GridDataServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*
        User user = null;
        if (Main.getContainerConfig().isAcEnabled()) {
            HttpSession session = request.getSession();
            user = (User) session.getAttribute("user");
            response.setHeader("Cache-Control", "no-store");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
        }
        */

        String sensor = HttpRequestUtils.getStringParameter("sensor", null, request);
        String from = HttpRequestUtils.getStringParameter("from", null, request);
        String to = HttpRequestUtils.getStringParameter("to", null, request);
        String xcol = HttpRequestUtils.getStringParameter("xcol", null, request);
        String ycol = HttpRequestUtils.getStringParameter("ycol", null, request);
        String timeformat = HttpRequestUtils.getStringParameter("timeformat", null, request);

        response.getWriter().write("sensor: " + sensor + "\n");
        response.getWriter().write("from: " + from + "\n");
        response.getWriter().write("to: " + to + "\n");
        response.getWriter().write("xcol: " + to + "\n");
        response.getWriter().write("ycol: " + to + "\n");
        response.getWriter().write("timeformat: " + to + "\n");
        
        response.getWriter().write(executeQuery("select * from "+sensor));


        /*
        for (String vsName : sensors) {
            if (!Main.getContainerConfig().isAcEnabled() || (user != null && (user.hasReadAccessRight(vsName) || user.isAdmin()))) {
                matchingSensors.append(vsName);
                matchingSensors.append(GetSensorDataWithGeo.SEPARATOR);
            }
        }
        */

    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

    public String executeQuery(String query) {

        Connection connection = null;
        StringBuilder sb= new StringBuilder();

        try {
            connection = Main.getDefaultStorage().getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            String s;

            // headers
            sb.append("# Query: " + query + "\n");
            sb.append("# ");

            byte typ[] = new byte[numCols];
            String columnLabel[] = new String[numCols];

            for (int col = 0; col < numCols; col++) {
                columnLabel[col] = metaData.getColumnLabel(col + 1);
                typ[col] = Main.getDefaultStorage().convertLocalTypeToGSN(metaData.getColumnType(col+1));
                // if (typ[col] == DataTypes.BINARY) logger.warn("type : BINARY");
                // convertLocalTypeToGSN
                // if (col < numCols - 1) sb.append(";");
            }
            // sb.append("\n");

            for (int row = 0; row < numRows; row++) {
                results.absolute(row + 1);                // Go to the specified row
                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column
                    if (o == null)
                        s = "null";
                    else
                        s = o.toString();
                        if (typ[col] == DataTypes.BINARY) {
                            byte[] bin = (byte[]) o;
                            sb.append(deserialize(bin));
                        }   else {
                            sb.append(columnLabel[col]+" "+s+"\n");
                        }
                }
                sb.append("\n");
            }
        }
        catch (SQLException e) {
            sb.append("ERROR in execution of query: " + e.getMessage());
        } finally {
            Main.getDefaultStorage().close(connection);
        }

        return sb.toString();
    }

    private String deserialize(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;

            in = new ObjectInputStream(bis);

            Double deserial[][] = new Double[0][];

            deserial = (Double[][]) in.readObject();
            in.close();

            logger.debug("deserial.length" + deserial.length);
            logger.debug("deserial[0].length" + deserial[0].length);

            for (int i = 0; i < deserial.length; i++) {

                for (int j = 0; j < deserial[0].length; j++) {
                    sb.append(deserial[i][j]).append(" ");
                }
                sb.append("\n");
            }

        } catch (IOException e) {
            logger.warn(e);
        }
        catch (ClassNotFoundException e) {
            logger.warn(e);
        }

        return sb.toString();
    }
}
