/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/http/ModelServlet.java
*
* @author Julien Eberle
*
*/

package gsn.http;

import com.thoughtworks.xstream.XStream;

import gsn.Main;
import gsn.Mappings;
import gsn.VirtualSensor;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
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
     * The result is a set of StreamElement in xml or json.
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
        String format = HttpRequestUtils.getStringParameter("format", null, request);
        
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
			int numParams = m.size()-2;
			if (format != null){
				numParams--;
			}
			DataField[] df = new DataField[numParams];
			Serializable[] sr = new Serializable[numParams];
			int i = 0;
			for(String k : m.keySet()){
				if(k.equals("vs") || k.equals("models") || k.equals("format")) continue;
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
	        if (!format.equalsIgnoreCase("json")){
		        str.append("<results>");
		        if (se != null){
		        for (StreamElement s : se){
		        	if (s != null){
		        	    str.append(xstream.toXML(new StreamElement4Rest(s)));
		        	}
		        }
		        }
		        str.append("</results>");
	        }else{
	        	str.append("[");
		        if (se != null){
		        for (StreamElement s : se){
		        	if (s != null){
		        		str.append("{timestamp:");
		        		str.append(s.getTimeStamp());
		        		str.append(",fields:[");
		        		for(int j=0;j<s.getData().length;j++){
		        			str.append("{name:\"");
		        			str.append(s.getFieldNames()[j]);
		        			str.append("\",type:\"");
		        			str.append(DataTypes.TYPE_NAMES[s.getFieldTypes()[j]]);
		        			str.append("\",value:\"");
		        			str.append(s.getData()[j]);
		        			str.append("\"},");
		        		}
		        		str.append("]},");
		        	}
		        }
		        }
		        str.append("]");
	        }
	        response.getWriter().write(str.toString());
		}
    }


    public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        doGet(request, res);
    }

}
