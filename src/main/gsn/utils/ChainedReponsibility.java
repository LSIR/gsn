package gsn.utils;

public abstract class ChainedReponsibility<T> {
  private ChainedReponsibility<T> next;

  public ChainedReponsibility<T> setNext(ChainedReponsibility<T> next){
    this.next = next;
    return next;
  }

  public ChainedReponsibility<T> getNext(){
    return next;
  }

  public boolean proccess(T t){
    if (handle(t))
        return getNext().proccess(t);
    return false;
  }

  protected abstract boolean handle(T t) ;
}
