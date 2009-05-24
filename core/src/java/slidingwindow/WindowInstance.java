package slidingwindow;

import gsn.beans.model.Window;
import gsn.windows.WindowInterface;
import gsn.beans.StreamElement;
import gsn.utils.EasyParamWrapper;
import gsn.utils.ParameterValidityCheck;

import java.util.List;

public class WindowInstance {
    private Window window;
    private WindowInterface windowHandler;

    public WindowInstance(Window window) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.window = window;
        // Todo: check to see all the required mandatory parameters in the window model are present in the window object.
        // Todo: data type check for the window parameters.
        ParameterValidityCheck.isValidParameters(window.getParameters());

        windowHandler = (WindowInterface) Class.forName(window.getModel().getClassName()).newInstance();

        boolean initialization = windowHandler.initialize(new EasyParamWrapper(window.getParameters()));
        // TODO: check on initialization.

        if (!initialization){
            //TODO: ERROR
            return;
        }
    }

    public void addDataToWindow(StreamElement se){
        windowHandler.postData(se);
    }

    public List<StreamElement> getWindow(){
        return windowHandler.getTotalContent();
    }
}
