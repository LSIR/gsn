package gsn.beans;

import gsn.beans.model.Parameter;
import gsn.beans.model.Window;

import java.util.ArrayList;
import java.util.List;

public class FixedSizeSortedWindow {
    private Window window;
    private ArrayList<StreamElement> dataList;

    public FixedSizeSortedWindow(Window window) {
        this.window = window;
        dataList = new ArrayList<StreamElement>();
    }

    public ArrayList<StreamElement> getDataList() {
        return dataList;
    }

    public int getSize() {
        List<Parameter> list = window.getParameters();
        for (Parameter parameter : list) {
            if ("size".equals(parameter.getModel().getName())) {
                return Integer.getInteger(parameter.getValue());
            }
        }
        return 0;
    }

    public void addElement(StreamElement se) {
        dataList.add(se);
    }
}
