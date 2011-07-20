package gsn.http;

import gsn.Mappings;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.StreamElement;
import gsn.vsensor.AbstractVirtualSensor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

/**
 * @todo validation & security part
 */

public class FieldUpload extends HttpServlet {
	static final long serialVersionUID = 13;
	private static final transient Logger logger = Logger.getLogger( StreamElement.class );
	   
	public void  doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		doPost(req, res);
	}
	
	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		String msg;
		Integer code;
		PrintWriter out = res.getWriter();
		ArrayList<String> paramNames = new ArrayList<String>();
		ArrayList<Serializable> paramValues = new ArrayList<Serializable>();
		
		//Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (!isMultipart) {
			out.write("not multipart!");
			code = 666;
			msg = "Error post data is not multipart!";
			logger.error(msg);
		} else {
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory();

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// Set overall request size constraint
//			upload.setSizeMax(5*1024*1024);
			
			
			List items;
			try {
				// Parse the request
				items = upload.parseRequest(req);
				//logger.info(items.toString());
				//building xml data out of the input
				String cmd = "";
				String vsname = "";
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
				    if (item.getFieldName().equals("vsname")){
				    	//define which cmd block is sent
				    	vsname = item.getString();
				    	//logger.info("vsname " + vsname);
				    } else if (item.getFieldName().equals("cmd")){
				    	//define which cmd block is sent
				    	cmd = item.getString();
				    	//logger.info("cmd " + cmd);
				    } else if (item.getFieldName().split(";")[0].equals(cmd)) {
				    	//only for the defined cmd
				    	
			    	    paramNames.add(item.getFieldName().split(";")[1]);
			    	    
			    	    if (item.isFormField()) {
			    	    	//logger.info("FORMFIELD FieldName " + item.getFieldName());
					    	paramValues.add(item.getString());
			    	    } else {
			    	    	//logger.info("NOT FORMFIELD FieldName " + item.getFieldName());
			    	    	paramValues.add(item);
			    	    }
				    }
				}
			
				//do something with xml aka statement.toString()
			
			    AbstractVirtualSensor vs = null;
			    boolean ret = false;
			    try {
			    	vs = Mappings.getVSensorInstanceByVSName( vsname ).borrowVS( );
			    	ret = vs.dataFromWeb( cmd , paramNames.toArray(new String[]{}) , paramValues.toArray(new Serializable[]{}) );
			    } catch ( VirtualSensorInitializationFailedException e ) {
			      logger.warn("Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
			    } finally {
			    	Mappings.getVSensorInstanceByVSName(vsname).returnVS(vs);
			    }
				
			    if (ret) {
					code = 200;
					msg = "The upload to the virtual sensor went successfully! ("+vsname+")";
			    }
			    else {
					code = 500;
					msg = "The upload could not be processed successfully! ("+vsname+")";
			    }
			} catch (ServletFileUpload.SizeLimitExceededException e) {
				code = 600;
				msg = "Upload size exceeds maximum limit!";
				logger.error(msg, e);
	        } catch(Exception e){
				code = 500;
				msg = "Internal Error: "+e;
				logger.error(msg, e);
			}
			
		}
		//callback to the javascript
		out.write("<script>window.parent.GSN.msgcallback('"+msg+"',"+code+");</script>");
	}
}