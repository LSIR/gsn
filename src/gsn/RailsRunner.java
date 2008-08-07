package gsn;

import gsn.beans.Modifications;
import gsn.beans.VSensorConfig;
import gsn.utils.graph.Graph;
import gsn.utils.graph.Node;

public class RailsRunner {
  public void start() {
    Thread[] threads = new Thread[1];
    threads[0] = new Thread() {
      public void run() {
        org.jruby.Main.main(new String[] {"web_app/web_app_jruby_start.rb"});
      }
    };

    for (Thread t : threads) t.start();

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
  public static void main(String[] args) {
    Graph<VSensorConfig> dependencyGraph = Modifications.buildDependencyGraphFromIterator(Mappings.getAllVSensorConfigs());
    
    for (Node<VSensorConfig> v: dependencyGraph.getNodes()) {
      System.out.println(v.toString());
    }
    new RailsRunner().start();
  }
}
