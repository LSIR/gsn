package gsn.core;

import gsn.ConfigurationVisitorAdapter;
import gsn.DBVisitor;
import gsn.beans.Operator;
import gsn.beans.SQLOperator;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.channels.DefaultDataChannel;
import gsn.utils.TableFinder;
import gsn.wrappers.Wrapper;
import gsn2.conf.ChannelConfig;
import gsn2.conf.OperatorConfig;
import gsn2.conf.SQLOperatorConfig;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.log4j.Logger;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import static org.picocontainer.behaviors.Behaviors.caching;
import static org.picocontainer.behaviors.Behaviors.synchronizing;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class OpLoader extends Thread {

	private static transient Logger                                logger                              = Logger.getLogger ( OpLoader.class );
public static void main(String[] args) {

    //		VirtualSensor sampleVS = createMock(VirtualSensor.class);
    MutablePicoContainer picoVS = new PicoBuilder().withBehaviors(synchronizing(), caching()).withLifecycle().build();

    MutablePicoContainer picoInputStream = new PicoBuilder(picoVS).withBehaviors(synchronizing(), caching()).withLifecycle().build();

    picoVS.addChildContainer(picoInputStream);
    MutablePicoContainer picoSource = new PicoBuilder(picoInputStream).withBehaviors(synchronizing(), caching()).withLifecycle().build();
    picoInputStream.addChildContainer(picoSource);
    MutablePicoContainer picoWrapper= new PicoBuilder(picoSource).withBehaviors(synchronizing(), caching()).withLifecycle().build();
    MutablePicoContainer picoWrapper2= new PicoBuilder(picoSource).withBehaviors(synchronizing(), caching()).withLifecycle().build();
    picoSource.addChildContainer(picoWrapper);
    picoSource.addChildContainer(picoWrapper2);

//    picoVS.addComponent(Operator.class, MockProcessingClass.class);
    picoVS.addComponent(new OperatorConfig());
//    picoVS.addComponent(new MockNonBlockingDataChannel());
    picoVS.setName("MyVsName");

    picoInputStream.addComponent(Operator.class, SQLOperator.class);
    picoInputStream.addComponent(SQLOperatorConfig.class,new SQLOperatorConfig());
    picoInputStream.addComponent(DataChannel.class, DefaultDataChannel.class);
    picoInputStream.addComponent(new ChannelConfig("InputStreamName","1","-1"));
    picoInputStream.setName("InputStream");

    picoSource.addComponent(Operator.class,SQLOperator.class);
    picoSource.addComponent(SQLOperatorConfig.class,new SQLOperatorConfig());
    picoSource.addComponent(DataChannel.class, DefaultDataChannel.class);
    picoSource.addComponent(new ChannelConfig("stream-source","1","-1"));
    picoSource.setName("Stream-Source");


//    picoWrapper.addComponent(Wrapper.class,MockWrapper.class);
    picoWrapper.addComponent(new WrapperConfig("wrapper1"));
    picoWrapper.addComponent(DataChannel.class,DefaultDataChannel.class);
    picoWrapper.addComponent(new ChannelConfig("wrapper","1","1"));
    picoWrapper.setName("WrapperName");

//    picoWrapper2.addComponent(Wrapper.class,MockWrapper2.class);
    picoWrapper2.addComponent(new WrapperConfig("wrapper2"));
    picoWrapper2.addComponent(DataChannel.class,DefaultDataChannel.class);
    picoWrapper2.addComponent(new ChannelConfig("wrapper","1","1"));
    picoWrapper2.setName("WrapperName2");

    Wrapper op = picoWrapper2.getComponent(Wrapper.class);
    System.out.println(op);
    picoVS.start();


//		System.out.println(picoSo);

    new ConfigurationVisitorAdapter(new DBVisitor()).traverse(picoVS);

    picoVS.dispose();

    try {
      printFromClause("select * from bla");
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void printFromClause(String sql) throws SQLException, JSQLParserException {

    Properties properties = new Properties();
    properties.put("user", "sa");
    properties.put("password","");
    String URL = "jdbc:h2:mem:v1";
    Connection c = DriverManager.getConnection(URL,properties);
    c.createStatement().execute("create table X()");

    properties = new Properties();
    properties.put("user", "sa");
    properties.put("password","");
    URL = "jdbc:h2:mem:v2";
    Connection c2 = DriverManager.getConnection(URL,properties);
    c2.createStatement().execute("create table X()");

    CCJSqlParserManager pm = new CCJSqlParserManager();
    Statement statement = pm.parse(new StringReader(sql));
    if (statement instanceof Select) {
      Select selectStatement = (Select) statement;
      TableFinder tablesFinder = new TableFinder();
      List tableList = tablesFinder.getTableList(selectStatement);
      for (Iterator iter = tableList.iterator(); iter.hasNext();) {
        System.out.println(iter.next());
      }
    }
  }


  private ArrayList<VSensorStateChangeListener> changeListeners = new ArrayList<VSensorStateChangeListener>();
  public void addVSensorStateChangeListener(VSensorStateChangeListener listener) {
    if (!changeListeners.contains(listener))
      changeListeners.add(listener);
  }

  public void removeVSensorStateChangeListener(VSensorStateChangeListener listener) {
    changeListeners.remove(listener);
  }

  private boolean fireVSensorLoading(OperatorConfig config) {
    for (VSensorStateChangeListener listener : changeListeners)
      if (!listener.vsLoading(config))
        return false;
    return true;
  }

  private boolean fireVSensorUnLoading(OperatorConfig config) {
    for (VSensorStateChangeListener listener : changeListeners)
      if (!listener.vsUnLoading(config)) {
        logger.error("Unloading failed !",new RuntimeException("Unloading : "+config.getIdentifier()+" is failed."));
        return false;
      }
    return true;
  }

}
