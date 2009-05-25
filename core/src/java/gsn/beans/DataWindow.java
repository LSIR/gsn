package gsn.beans;

import gsn.beans.model.Parameter;
import gsn.beans.model.Window;

import java.util.ArrayList;
import java.util.List;

public class DataWindow {
    private Window window;
    private ArrayList<StreamElement> dataList;

    public DataWindow(Window window) {
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
                return Integer.parseInt(parameter.getValue());
            }
        }
        return 0;
    }

    public void addElement(StreamElement se) {
        dataList.add(se);
    }

    public int getElementCount() {
        return dataList.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("window size: ").append(getSize()).append(", data=").append(dataList.toString());
        return sb.toString();
    }
}
