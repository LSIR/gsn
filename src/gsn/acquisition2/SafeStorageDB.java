package gsn.acquisition2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class SafeStorageDB {
  
  public static transient Logger logger= Logger.getLogger ( SafeStorageDB.class );
  
  private Connection connection;
  
  private String dbUrl = null;
  
  public SafeStorageDB(int safeStoragePort) throws ClassNotFoundException, SQLException {
    Class.forName("org.h2.Driver");
    dbUrl = "jdbc:h2:storage" + safeStoragePort + ".h2";
    connection = getConnection();
  }
  
  private void close(Connection c) {
    try {
      if (c!=null && !c.isClosed())
        c.close();
    }catch (Exception e) {
    }
  }
  
  public Connection getConnection() throws SQLException {
    if (connection ==null || connection.isClosed())
     connection = DriverManager.getConnection(dbUrl, "sa", "");
    return connection;
  }
  
  
  
  public void executeSQL(String string) throws SQLException {
    Statement stmt = connection.createStatement();
    stmt.execute(string);
    stmt.close();
  }
  
  public String prepareTableIfNeeded(String requester) throws SQLException {
    final Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("select table_name from SETUP where requester = '"+requester+"'");
    String toReturn = null;
    if (rs.next()) { //exists
      toReturn =rs.getString(1);
    }
    else { //create
      toReturn  = "_"+Integer.toString((int)(Math.random()*10000000));
      //create the table to store the data
      stmt.execute("create table "+toReturn+" (pk bigint not null identity primary key, processed boolean not null default false, stream_element ARRAY not null, created_at timestamp not null default CURRENT_TIMESTAMP())");
      PreparedStatement ps = connection.prepareStatement("insert into setup(table_name,requester) values (?,?) ");
      ps.setString(1,toReturn);
      ps.setString(2, requester);
      ps.execute();
      ps.close();
    }
    stmt.close();
    return toReturn;
  }



  public PreparedStatement createPreparedStatement(String sqlCommand) throws SQLException {
    PreparedStatement ps = getConnection().prepareStatement(sqlCommand);
    return ps;
  }
}
