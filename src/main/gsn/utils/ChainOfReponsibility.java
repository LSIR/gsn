package gsn.utils;

public abstract class ChainOfReponsibility<T> {
  private ChainOfReponsibility<T> next;

  public ChainOfReponsibility<T> setNext(ChainOfReponsibility<T> next){
    this.next = next;
    return next;
  }

  public ChainOfReponsibility<T> getNext(){
    return next;
  }

  public boolean proccess(T t){
    if (handle(t)){
      if (getNext()!=null)
        return getNext().proccess(t);
      return true;
    }
    return false;
  }

  protected abstract boolean handle(T t) ;
}
