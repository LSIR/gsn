package gsn.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class BetterQueue<T> {
    private Queue<T> queue = new LinkedList<T>();
    private int insertCounter;
    private long lastInsert = -1;
    private long firstInsert = -1;
    private long lastRemove = -1;
    private long firstRemove = -1;

    private ArrayList<QueueChangeListener> listeners = new ArrayList<QueueChangeListener>();

    public void resetCounter() {
        insertCounter = 0;
    }

    public Queue getQueue() {
        return queue;
    }

    public long getLastInsert() {
        return lastInsert;
    }

    public long getFirstInsert() {
        return firstInsert;
    }

    public long getLastRemove() {
        return lastRemove;
    }

    public long getFirstRemove() {
        return firstRemove;
    }

    public int getInsertCounter() {
        return insertCounter;
    }

    public void addListener(QueueChangeListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(QueueChangeListener<T> listener) {
        listeners.remove(listener);
    }

    /**
     * *********************************************
     * Events
     * **********************************************
     */

    public void fireAddItemListener(T object) {
        for (QueueChangeListener listener : listeners)
            listener.itemAdded(object);
    }

    public void fireRemovedItemListener(T object) {
        for (QueueChangeListener listener : listeners)
            listener.itemRemove(object);
    }

    public void fireEmptyListener(T object) {
        for (QueueChangeListener listener : listeners)
            listener.queueEmpty();
    }

    public void fireNotEmptyListener(T object) {
        for (QueueChangeListener listener : listeners)
            listener.queueNotEmpty();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean contains(T t) {
        return queue.contains(t);
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Iterator<T> it = queue.iterator();

            public boolean hasNext() {
                return it.hasNext();
            }

            public T next() {
                return it.next();
            }

            public void remove() {
                throw new RuntimeException("Remove operation on the Queue is not supported.");
            }
        };
    }

    public void clear() {
        while (!isEmpty())
            dequeue();
    }

    public boolean add(T t) {
        insertCounter++;
        lastInsert = System.currentTimeMillis();
        if (firstInsert == -1)
            firstInsert = lastInsert;
        boolean isEmpty = queue.isEmpty();
        boolean toReturn = queue.offer(t);
        if (isEmpty)
            fireNotEmptyListener(t);
        fireAddItemListener(t);
        return toReturn;
    }


    public T dequeue() {
        if (isEmpty())
            return null;
        lastRemove = System.currentTimeMillis();
        if (firstRemove == -1)
            firstRemove = lastRemove;

        T toReturn = queue.poll();
        if (queue.isEmpty())
            fireEmptyListener(toReturn);
        fireRemovedItemListener(toReturn);
        return toReturn;
    }

    public T element() {
        return queue.element();
    }

    public T peek() {
        return queue.peek();
    }

}
