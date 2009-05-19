package gsn.beans.decorators;

import gsn.beans.QueueChangeListener;
import gsn.beans.model.DataNodeInterface;

import java.util.concurrent.LinkedBlockingQueue;

public class QueryDecorator extends ThreadDataNodeDecorator {

    private LinkedBlockingQueue lockQueue = new LinkedBlockingQueue();

    public QueryDecorator(QueueDataNodeDecorator node) {
        super(node);
    }


    protected void initializeBlockingQueue() {
        for (DataNodeInterface child : getChildren()) {
            QueueDataNodeDecorator childDec = (QueueDataNodeDecorator) child;
            childDec.getQueue(this).addListener(new QueueChangeListener() {

                public void itemAdded(Object obj) {

                }

                public void itemRemove(Object obj) {

                }

                public void queueEmpty() {
                    lockQueue.poll();
                }

                public void queueNotEmpty() {
                    lockQueue.add(Boolean.TRUE);
                }
            });
        }
    }

    public void run() {
        if (lockQueue.isEmpty()) {
            try {
                lockQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lockQueue.add(Boolean.TRUE);
            }
            // There is something to be processed.
            // insert the final output into my window.
            // distribute my window to the parent nodes.

        }
    }
}
