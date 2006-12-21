package gsn.web;

import gsn.beans.StreamElement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

/**
 * @author Cl�ment Beffa (clement.beffa@epfl.ch)<br>
 * @web.servlet name="FieldUpload" load-on-startup="true"
 * @web.servlet-mapping url-pattern="/upload"
 * 
 * @todo validation & security part
 */

public class FieldUpload extends HttpServlet {
	static final long serialVersionUID = 13;
	private static final transient Logger logger = Logger.getLogger( StreamElement.class );
	   
	public void  doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		doPost(req, res);
	}
	
	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		PrintWriter out = res.getWriter();
		
		//Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (!isMultipart) {
			out.write("not multipart!");
			logger.error("not multipart");
			return;
		}
		
		//Create a factory for disk-based file items
		FileItemFactory factory = new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		//Set overall request size constraint
		//upload.setSizeMax(1000);
		
		// Parse the request
		List /* FileItem */ items;
		try {
			items = upload.parseRequest(req);
		} catch(Exception e){
			logger.error("exception:",e);
			return;
		}
		
		// Process the uploaded items
		Iterator iter = items.iterator();
		String cmd = "";
		Base64 b64 = new Base64();
		StringBuilder sb = new StringBuilder("<input>\n" );
		while (iter.hasNext()) {
		    FileItem item = (FileItem) iter.next();
		    
		    if (item.getFieldName().equals("vsname")){
		    	//define which cmd block is sent
		    	sb.append("<vsname>"+item.getString()+"</vsname>\n");
		    } else if (item.getFieldName().equals("cmd")){
		    	//define which cmd block is sent
		    	cmd = item.getString();
		    	sb.append("<command>"+item.getString()+"</command>\n");
		    	sb.append("<fields>\n");
		    } else if (item.getFieldName().split(";")[0].equals(cmd)) {
		    	//only for the defined cmd
		    	
		    	sb.append("<field>\n");
	    	    sb.append("<name>"+item.getFieldName().split(";")[1]+"</name>\n");
	    	    if (item.isFormField()) {
			    	sb.append("<value>"+item.getString()+"</value>\n");
	    	    } else {
	    	    	sb.append("<value>"+new String(b64.encode(item.get()))+"</value>\n");
	    	    }
	    	    sb.append("</field>\n");
		    }
		}
		sb.append("</fields>\n");
		sb.append("</input>\n" );
		
		
		
		//if no error, send a successful redirect
	    //res.sendRedirect("/#home,msg=upsucc");
		out.write(sb.toString());
	}
}