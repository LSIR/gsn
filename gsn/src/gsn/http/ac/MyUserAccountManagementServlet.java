package gsn.http.ac;

import gsn.Main;
import gsn.beans.ContainerConfig;


import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 27, 2010
 * Time: 2:45:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyUserAccountManagementServlet   extends HttpServlet
{
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/

    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        // Get the session
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null)
       {
           this.redirectToLogin(req,res);
       }
       else
       {
           this.checkSessionScheme(req,res);
           this.printHeader(out);
           this.printLayoutMastHead(out, user );
           this.printLayoutContent(out);
           this.printUserAccountLinks(out, user);
           this.printLayoutFooter(out);
  
       }
    }
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
       this.doGet(req,res);
    }


    /****************************************** HTML Printing Methods*******************************************/
    /***********************************************************************************************************/

    private void printHeader(PrintWriter out)
	{
        out.println("<HTML>");
        out.println("<HEAD>");
		out.println("<TITLE>User Account Management</TITLE>");
        out.println(" <link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/style/acstyle.css\"/>");
        //printStyle(out);
        out.println("</HEAD>");
        out.println("<body>");
        out.println("<div id=\"container\">");
        out.println("<div class=box>");

	}
   private void printLayoutMastHead(PrintWriter out, User user)
    {
       out.println("<div id=\"masthead\">");
       out.println("<h1><a id=\"gsn-name\" style=\"\" href=\"/\">" + Main.getContainerConfig( ).getWebName( ) + "</a></h1>");
       out.println("<div id=\"breadcrumbnav\"><a href=\"http://www.permasense.ch\">PermaSense</a> > <a id=\"gsn-name\" style=\"\" href=\"/\">GSN</a> > <a href=/gsn/MyAccessRightsManagementServlet>Access Rights</a> > User Account</div>");

       out.println("</div>");
       out.println("<div id=\"navigation\">");
       out.println("<div id=\"menu\">");
       this.printLinks(out);
       out.println("</div>");
       out.println("<div id=\"logintext\">");
       this.printUserName(out, user);
       out.println("</div>");
       out.println("</div>");
    }
    private void printLayoutContent(PrintWriter out)
    {
        out.println("<div id=\"content\">");
    }
    private void printLayoutFooter(PrintWriter out)
    {
        out.println("</div>");
        out.println("<div class=\"separator\">");
        out.println("<div id=\"footer\">");
        out.println("<table width=\"100%\"><tr>");
        out.println("<td style=\"width:50%;color:#444444;font-size:12px;line-height:1.4em;\"><b>A Project of <a href=\"http://www.ethz.ch\" target=\"_blank\">ETH Zurich</a>, <a href=\"http://www.unibas.ch\" target=\"_blank\">Uni Basel</a> and <a href=\"http://www.uzh.ch\" target=\"_blank\">Uni Zurich</a></b></td>");
        out.println("<td style=\"text-align:right;width:50%;font-size:9px;color:#666666;\">Powered by <a href=\"http://gsn.sourceforge.net/\">GSN</a>,  Distributed Information Systems Lab, EPFL 2006</td>");
		out.println("</tr></table>");
        out.println("</div>");//footer
        out.println("</div>");//separator
        out.println("</div>");//box
        out.println("</div>");//container
        out.println("</body>");
        out.println("</html>");
    }

    private void printLinks(PrintWriter out)
    {
        //out.println("<a class=linkclass href=\"/gsn/MyLoginHandlerServlet\">login</a>");
        out.println("<li><a href=\"/\">Home</a></li>");
        out.println("<li><a href=/gsn/MyAccessRightsManagementServlet>access rights</a></li>");
        out.println("<li class=\"selected\"><a href=/gsn/MyUserAccountManagementServlet>user account</a></li>");


    }
    private void printUserName(PrintWriter out, User user)
    {
        //String username=user.getUserName();
        out.println("<li><a href=\"/gsn/MyLogoutHandlerServlet\">logout</a></li>");
        out.println("<li><div id=\"logintextprime\">logged in as: "+user.getUserName()+"</div></li>");
    }
    public void printUserAccountLinks(PrintWriter out, User user)
    {
        out.println("<p>Welcome to your account management ! you have the following options:</p>");
        if (user != null) {
	        out.println("<ul class=linklistul >");
	        out.println("<li class=linklistli><a href=\"/gsn/MyUserDetailUpdateServlet\">Update your user details</a></LI>");
	        if (!user.isAdmin())
	        	out.println("<LI class=linklistli><a href=\"/gsn/MyUserUpdateServlet\">Update your access rights</a></LI>");
	        out.println("<LI class=linklistli><a href=\"/gsn/MyDataSourceCandidateRegistrationServlet\">Upload new virtual sensor files</a></LI>");
	        if (!user.isAdmin())
	        	out.println("<LI class=linklistli><a href=\"/gsn/MyOwnerWaitingListServlet\">Check the waiting list for your virtual sensor(s)</a></LI>");
	        out.println("</ul>");
        }
    }

    /****************************************** Client Session related Methods*******************************************/
    /********************************************************************************************************************/


    private void checkSessionScheme(HttpServletRequest req, HttpServletResponse res)throws IOException
    {

         if(req.getScheme().equals("https")== true)
        {
            if((req.getSession().getAttribute("scheme")==null))
            {
                req.getSession().setAttribute("scheme","https");
            }
        }
         else if(req.getScheme().equals("http")== true )
        {
             if((req.getSession().getAttribute("scheme")==null))
            {
                req.getSession().setAttribute("scheme","http");
            }
             
            String remoteHost = req.getHeader("x-forwarded-for");
            if (remoteHost == null) {
                remoteHost = req.getHeader("X_FORWARDED_FOR");
                if (remoteHost == null) {
                    remoteHost = req.getRemoteHost();
                }
            }
            res.sendRedirect("https://"+remoteHost+"/gsn/MyUserAccountManagementServlet");

        }
    }
    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res)throws IOException
    {
        req.getSession().setAttribute("login.target", HttpUtils.getRequestURL(req).toString());
        res.sendRedirect("/gsn/MyLoginHandlerServlet");
    }


}
