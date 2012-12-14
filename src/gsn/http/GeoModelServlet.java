package gsn.http;

import com.thoughtworks.xstream.XStream;
import gsn.Main;
import gsn.Mappings;
import gsn.VirtualSensor;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.DataField;
import gsn.beans.StreamElement;

import gsn.http.ac.User;
import gsn.http.rest.StreamElement4Rest;
import gsn.utils.models.AbstractModel;
import gsn.vsensor.AbstractVirtualSensor;
import gsn.vsensor.ModellingVirtualSensor;

import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;




public class GeoModelServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(GeoModelServlet.class);
    private User user = null;
    
    private XStream xstream = StreamElement4Rest.getXstream();

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    AbstractModel modelClass = null;
        if (Main.getContainerConfig().isAcEnabled()) {
            HttpSession session = request.getSession();
            user = (User) session.getAttribute("user");
            response.setHeader("Cache-Control", "no-store");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
        }

        String model = HttpRequestUtils.getStringParameter("models", null, request);
        String vsname = HttpRequestUtils.getStringParameter("vs", null, request);
        
		if (model != null && vsname != null){
			try {
			    VirtualSensor vs = Mappings.getVSensorInstanceByVSName(vsname);
			    if (vs == null){
			    	logger.error("can't find VS: "+ vsname);
					response.sendError(404);
					return;
			    }
			    AbstractVirtualSensor avs = vs.borrowVS();
				if (avs instanceof ModellingVirtualSensor){
					modelClass = ((ModellingVirtualSensor)avs).getModel(model)[0];
				}
				vs.returnVS(avs);
			} catch (VirtualSensorInitializationFailedException e) {
				logger.error("Error loading the model for " + model);	
			}
		}
		if (modelClass == null){
			response.sendError(404);
		}else{
			Map<String, String[]> m = request.getParameterMap();
			
			DataField[] df = new DataField[m.size()-2];
			Serializable[] sr = new Serializable[m.size()-2];
			int i = 0;
			for(String k : m.keySet()){
				if(k.equals("vs") || k.equals("models")) continue;
				try{
				df[i] = new DataField(k,"double"); // !!! _HARDCODED, only supports double
				sr[i] = Double.parseDouble(m.get(k)[0]);
				i ++;
				}catch(Exception e){
					logger.error("Error getting parameter " + k + " :" + m.get(k)[0]);
				}
			}
			 
			StreamElement[] se = modelClass.query(new StreamElement(df,sr));
	        
	        StringBuilder str = new StringBuilder();
	        
	        str.append("<Results>");
	        if (se != null){
	        for (StreamElement s : se){
	        	if (s != null){
	        	    str.append(xstream.toXML(new StreamElement4Rest(s)));
	        	}
	        }
	        }
	        str.append("</Results>");
	        response.getWriter().write(str.toString());
		}
    }


    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

}
