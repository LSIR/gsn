package gsn.core;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.IOCase;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.io.File;
import java.io.FileFilter;

public class DirectoryMonitor extends TimerTask{
  private String path;
  private int monitoringInterval = 5000; //5 seconds.
  private ArrayList<FilePresenceListener> listeners = new ArrayList();
  private Timer timer;
  private HashMap<String,Long> changes = new HashMap<String,Long>();

  private File dir;

  public DirectoryMonitor(String path, int monitoringInterval) {
    this.path = path;
    this.monitoringInterval = monitoringInterval;
    dir = new File(path);
    if(! dir.isDirectory())
      throw new RuntimeException("The specified directory:"+path+" doesn't exist.");
    timer = new Timer(this.getClass().getName());
  }

  public void addListener(FilePresenceListener listener){
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  public void removeListener(FilePresenceListener listener){
    listeners.remove(listener);
  }

  public void start() {
    timer.schedule(this,0,monitoringInterval);
  }

  public void stop(){
    timer.cancel();
  }

  public void run() {
    HashMap<String,Long> localChanges = new HashMap<String,Long>();
    File[] files = dir.listFiles();
    for (File fileName: files){
      if (fileName.getAbsolutePath().toLowerCase().endsWith(".xml"))
        localChanges.put(fileName.getAbsolutePath(),fileName.lastModified());
    }
    Collection<String> addition = CollectionUtils.subtract(localChanges.keySet(), changes.keySet());
    Collection<String> removal = CollectionUtils.subtract( changes.keySet(),localChanges.keySet());

    for (String removedFile:removal)
      for (FilePresenceListener listener: listeners)
        listener.fileRemoval(removedFile);

    for (String addedFile:addition)
      for (FilePresenceListener listener: listeners)
        listener.fileAddition(addedFile);

    Collection intersection = CollectionUtils.intersection(localChanges.keySet(), changes.keySet());
    for (Object sameName: intersection)  {
      if (localChanges.get(sameName) >0 && !localChanges.get(sameName).equals(changes.get(sameName))){
        for (FilePresenceListener listener: listeners)
          listener.fileChanged((String) sameName);
      }
    }
    changes = localChanges;
  }
}
