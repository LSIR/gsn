package gsn;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.tools.ant.taskdefs.Sleep;

import gsn.beans.Modifications;
import gsn.beans.VSensorConfig;
import gsn.utils.graph.Graph;
import gsn.utils.graph.Node;

public class RailsRunner {
  public void start() {
    Graph<VSensorConfig> dependencyGraph = Modifications.buildDependencyGraphFromIterator(Mappings.getAllVSensorConfigs());
    for (Node<VSensorConfig> v: dependencyGraph.getNodes()) {
      System.out.println(v.toString());
    }
    Thread[] threads = new Thread[1];
    threads[0] = new Thread() {
      public void run() {
        try {
          org.jruby.Main.main(new String[] {"web_app/web_app_jruby_start.rb"});
        }catch (Exception e) {
          e.printStackTrace();
        }
      }
    };

    for (Thread t : threads) t.start();
    while(!isRunning())
      try {
        System.out.println("Waiting for HTTP Server to Start ...");
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("HTTP Server to Start [ok]");
  }


  public void stop() {
    Thread[] threads = new Thread[1];
    threads[0] = new Thread() {
      public void run() {
        org.jruby.Main.main(new String[] {"web_app/web_app_jruby_stop.rb"});
      }
    };
    for (Thread t : threads) t.start();
  }

  public boolean isRunning() {
    try {
      URL url = new URL("http://localhost:3000");
      url.openStream();
      return true;
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      return false;
    }
  }
  public static void main(String[] args) {
    new RailsRunner().start();
  }
}
