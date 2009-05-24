package gsn.beans;

import gsn.beans.model.DataNodeInterface;

import java.util.List;

public class DataDispatcher {

    private List<DataNodeInterface> consumers;

    public DataDispatcher(List<DataNodeInterface> parents) {
        this.consumers = parents;
    }

    public void addStreamElement(StreamElement se){
            for (DataNodeInterface node : consumers)
                ; // Consume !
    }
}
