package gsn.wrappers;

public abstract class RubyWrapperAdapter  extends AbstractWrapper{

  public boolean initialize() {
    return bootstrap();
  }

  public void finalize() {
    shutdown();
  }

  public abstract  boolean bootstrap() ;
  
  public abstract void shutdown();

}
