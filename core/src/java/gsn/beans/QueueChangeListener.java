package gsn.beans;

public interface QueueChangeListener<T> {
    public void itemAdded(T obj);

    public void itemRemove(T obj);

    public void queueEmpty();

    public void queueNotEmpty();
}
