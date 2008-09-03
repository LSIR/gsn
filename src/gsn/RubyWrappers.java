package gsn;

public class RubyWrappers {
  public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    org.jruby.Main.main(new String[] {"src/gsn/ruby/load_wrappers.rb"});  
    Object w = Class.forName("RubyWrappers2").newInstance(); // have to wait until jruby 1.2
  }
}
