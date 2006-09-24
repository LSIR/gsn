package gsn.web ;

import gsn.storage.StorageManager ;

import java.io.IOException ;

import javax.servlet.ServletException ;
import javax.servlet.http.HttpServlet ;
import javax.servlet.http.HttpServletRequest ;
import javax.servlet.http.HttpServletResponse ;

//path="/WEB-INF/file_not_found.jpg"
/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Create : 2006 <br>
 *         Created for : GSN project. <br>
 * @web.servlet name="BinaryDownload" load-on-startup="true"
 * @web.servlet-mapping url-pattern="/field"
 * 
 * 
 */
public class FieldDownloadServlet extends HttpServlet {

   static final String prefix = "select * from " ;

   static final String postfix = " where PK = ? " ;

   public void doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
      String vs_name = req.getParameter ( "vs" ).trim ( ) ;
      String primaryKey = req.getParameter ( "identity" ).trim ( ) ;
      String colName = req.getParameter ( "field" ).trim ( ) ;
      String type = req.getParameter ( "type" ) ;
      if ( primaryKey == null || colName == null || vs_name == null )
         return ;
      if ( type == null )
         type = "" ;
      else
         type = type.trim ( ) ;
      StringBuilder query = new StringBuilder ( ).append ( prefix ).append ( vs_name ).append ( postfix ) ;
      byte [ ] picture = StorageManager.getInstance ( ).getBinaryFieldByQuery ( query , colName , Long.parseLong ( primaryKey ) ) ;
      if ( picture == null )
         // TODO return mapping.findForward ( "fail" ) ;
         return ;

      if ( type.equalsIgnoreCase ( "svg" ) )
         res.setContentType ( "image/svg+xml" ) ;
      res.getOutputStream ( ).write ( picture ) ;
   }

   public void doPost ( HttpServletRequest arg0 , HttpServletResponse arg1 ) throws ServletException , IOException {
      doGet ( arg0 , arg1 ) ;
   }

}
