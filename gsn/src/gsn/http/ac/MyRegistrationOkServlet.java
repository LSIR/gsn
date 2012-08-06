package gsn.http.ac;

import gsn.Main;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: May 5, 2010
 * Time: 4:02:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyRegistrationOkServlet  extends HttpServlet
{
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/
    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
        res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		printRegistrationOk(out);


    }
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
        doGet(req,res);

    }
    /****************************************** HTML Printing Methods*******************************************/
    /***********************************************************************************************************/


    private void printHeader(PrintWriter out)
	{
        out.println("<HTML>");
        out.println("<HEAD>");
        //For Java Script!!
        //this.printEmbeddedJS(out);
        out.println("<script type=\"text/javascript\" src=\"/js/acjavascript.js\"></script>");
		out.println("<TITLE>Sign Up Form</TITLE>");
        out.println(" <link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/style/acstyle.css\"/>");
        //printStyle(out);
        out.println("</HEAD>");
        out.println("<body>");

        out.println("<div id=\"container\">");
        out.println("<div class=box>");

	}
   private void printLayoutMastHead(PrintWriter out)
   {
       out.println("<div id=\"masthead\">");
       out.println("<h1><a id=\"gsn-name\" style=\"\" href=\"/\">" + Main.getContainerConfig( ).getWebName( ) + "</a></h1>");

       out.println("</div>");
       out.println("<div id=\"navigation\">");
       out.println("<div id=\"menu\">");
       this.printLinks(out);
       out.println("</div>");
       out.println("</div>");
   }
    
   private void printLayoutContent(PrintWriter out)
   {
       out.println("<div id=\"content\">");
   }

   private void printLinks(PrintWriter out)
   {
       out.println("<a class=linkclass href=\"/\">GSN home</a>");
   }

   private void printLayoutFooter(PrintWriter out)
   {
       out.println("</div>");//content
       out.println("<div class=\"separator\">");
       out.println("<div id=\"footer\">");
       out.println("<table width=\"100%\"><tr>");
       out.println("<td style=\"width:50%;color:#444444;font-size:12px;line-height:1.4em;\"><b>A Project of <a href=\"http://www.ethz.ch\" target=\"_blank\">ETH Zurich</a>, <a href=\"http://www.unibas.ch\" target=\"_blank\">Uni Basel</a> and <a href=\"http://www.uzh.ch\" target=\"_blank\">Uni Zurich</a></b></td>");
       out.println("<td style=\"text-align:right;width:50%;font-size:9px;color:#666666;\">Powered by <a href=\"http://gsn.sourceforge.net/\">GSN</a>,  Distributed Information Systems Lab, EPFL 2006</td>");
		out.println("</tr></table>");
       out.println("</div>");//footer
       out.println("</div>");//separator
       //out.println("</div>");//box
       out.println("</div>");//container
       out.println("</body>");
       out.println("</html>");
       out.println("<BR>");
       //out.println("<HR>");
   }
    private void printRegistrationOk(PrintWriter out)
    {
        printHeader(out);
        printLayoutMastHead(out);
        printLayoutContent(out);
        out.println("<p>GSN access have received your registration request!<BR>\n" +
                "You will receive a confirmation E-mail containing your account information in 24 hours. </p>");
        printLayoutFooter(out);
    }
}
