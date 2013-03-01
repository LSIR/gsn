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




public class ModelServlet extends HttpServlet {

    private static transient Logger logger = Logger.getLogger(ModelServlet.class);
    private User user = null;
    
    private XStream xstream = StreamElement4Rest.getXstream();

    /**
     * Query the given model in the given modeling virtual sensor, using all the other parameters that can be cast to integers or double
     * The parameter vs is the name of the modeling virtual sensor and is mandatory. If the virtual sensor doesn't exists, it return a 404 error.
     * The parameter models is the index of the model within the VS. If not specified it is 0.
     * All the other parameters are used to build the query, if their value can be cast to int or double.
     * The result is a set of StreamElement in xml.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    AbstractModel modelClass = null;
        if (Main.getContainerConfig().isAcEnabled()) {
            HttpSession session = request.getSession();
            user = (User) session.getAttribute("user");
            response.setHeader("Cache-Control", "no-store");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
        }

        int model = HttpRequestUtils.getIntParameter("model", 0, request);
        String vsname = HttpRequestUtils.getStringParameter("vs", null, request);
        
		if (vsname != null){
			try {
				if (!Main.getContainerConfig().isAcEnabled() || (user != null && (user.hasReadAccessRight(vsname) || user.isAdmin()))) {
				    VirtualSensor vs = Mappings.getVSensorInstanceByVSName(vsname);
				    if (vs == null){
				    	logger.error("can't find VS: "+ vsname);
						response.sendError(404);
						return;
				    }
				    AbstractVirtualSensor avs = vs.borrowVS();
					if (avs instanceof ModellingVirtualSensor){
						modelClass = ((ModellingVirtualSensor)avs).getModel(model);
					}
					vs.returnVS(avs);
				}
				else{
					logger.error("Unauthorized access to "+ vsname);
					response.sendError(401);
					return;
				}
			} catch (VirtualSensorInitializationFailedException e) {
				logger.error("Error in http request loading the model for " + model);	
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
					sr[i] = Integer.parseInt(m.get(k)[0]);
					df[i] = new DataField(k,"integer");
					i ++;
				}
				catch(NumberFormatException e){
					try{
						sr[i] = Double.parseDouble(m.get(k)[0]);
						df[i] = new DataField(k,"double");
						i ++;
					}
					catch(NumberFormatException ee){
						logger.error("Error in http request getting parameter " + k + " :" + m.get(k)[0]);
					}
				}
			}
			 
			StreamElement[] se = modelClass.query(new StreamElement(df,sr));
	        
	        StringBuilder str = new StringBuilder();
	        
	        str.append("<results>");
	        if (se != null){
	        for (StreamElement s : se){
	        	if (s != null){
	        	    str.append(xstream.toXML(new StreamElement4Rest(s)));
	        	}
	        }
	        }
	        str.append("</results>");
	        response.getWriter().write(str.toString());
		}
    }


    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

}
