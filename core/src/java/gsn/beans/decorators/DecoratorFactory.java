package gsn.beans.decorators;

import gsn.beans.model.DataNodeInterface;

public interface DecoratorFactory {
    DataNodeInterface createDecorator(DataNodeInterface node);
}
