package gsn.http.ac;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 20, 2010
 * Time: 8:01:31 PM
 * To change this template use File | Settings | File Templates.
 */
import gsn.Main;
import gsn.http.WebConstants;
import org.apache.log4j.Logger;


import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

public class MyGroupManagementServlet extends HttpServlet
{

    private static transient Logger logger                             = Logger.getLogger( MyGroupManagementServlet.class );
    /****************************************** Servlet Methods*******************************************/
    /******************************************************************************************************/
    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
        Vector groupList=null;
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        ConnectToDB ctdb = null;

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
            if(user.isAdmin()== false)
           {
               res.sendError( WebConstants.ACCESS_DENIED , "Access denied." );
           }
           else
           {
               this.setSessionPrintWriter(req,out);
               
            try
            {
                ctdb = new ConnectToDB();
                this.printHeader(out);
                this.printLayoutMastHead(out,user);
                this.printLayoutContent(out);
                groupList=ctdb.getGroupList();
                for(int i=0;i<groupList.size();i++)
                {
                    
                    printGroupInformation(out,(Group)(groupList.get(i)));

                }
                out.println("<div class=\"spacer\"></div>");
                if(groupList.size()==0)
                {
                    out.println("<p><B> There is no entry in the Group List ! </B></p>");
                }
            }
            catch(Exception e)
            {

                logger.error("ERROR IN doGet");
			    logger.error(e.getMessage(),e);
            }
            finally
            {
                if(ctdb!=null)
                {
                    ctdb.closeStatement();
                    ctdb.closeConnection();
                }
            }
               
          }
       }
        this.printLayoutFooter(out);
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
		out.println("<TITLE>Group Management</TITLE>");
        out.println(" <link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/style/acstyle.css\"/>");
        //printStyle(out);
        out.println("</HEAD>");
        //out.println("<body>");
        out.println("<body onload=\"loadScroll()\" onunload=\"saveScroll()\" >");
        out.println("<div id=\"container\">");
        out.println("<div class=box>");

	}
    private void printLayoutMastHead(PrintWriter out, User user)
    {
        out.println("<div id=\"masthead\">");
        out.println("<h1><a id=\"gsn-name\" style=\"\" href=\"/\">" + Main.getContainerConfig( ).getWebName( ) + "</a></h1>");
        out.println("<div id=\"breadcrumbnav\"><a href=\"http://www.permasense.ch\">PermaSense</a> > <a id=\"gsn-name\" style=\"\" href=\"/\">GSN</a> > <a href=/gsn/MyAccessRightsManagementServlet>Access Rights</a> > <a href=/gsn/MyAdminManagementServlet>Admin</a> > Group Management</div>");

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
        out.println("</div>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
    private void printLinks(PrintWriter out)
    {
        out.println("<li><a href=\"/\">Home</a></li>");
        out.println("<li><a href=/gsn/MyAccessRightsManagementServlet>access rights</a></li>");
        out.println("<li><a href=/gsn/MyAdminManagementServlet>admin</a></li>");
        out.println("<li class=\"selected\"><a href=/gsn/MyGroupManagementServlet>group management</a></li>");
    }
    private void printUserName(PrintWriter out, User user)
    {
        //String username=user.getUserName();
        out.println("<li><a href=\"/gsn/MyLogoutHandlerServlet\">logout</a></li>");
        out.println("<li><div id=\"logintextprime\">logged in as: "+user.getUserName()+"</div></li>");
    }


    private void printGroupInformation(PrintWriter out,Group group) throws ServletException
	{
        out.println("<h2>New Group Entry </h2>");
        out.println("<table class=tab>");
        out.println("<tr><th> group name </th>");
        out.println("<th> group structure</th>");
        out.println("<th> admin decision</th>");
        out.println("<th> admin decision</th></tr>");
        out.println("<tr>");
        this.printInputs(out,group);
        this.printGroupStructureLink(out, group.getGroupName());
        this.printForms(out,group.getGroupName());
        out.println("</tr>");
        out.println("</table>");
        out.println("<br>");
    }
    private void printGroupStructureLink(PrintWriter out, String groupname)
    {
        String groupurl="/gsn/MyGroupHtmlResultSetServlet?groupname="+groupname;
        out.println("<ul class=displaylinkul >");
        out.println("<td style=text-align:center><LI class=displaylinkli><a href="+groupurl+" onClick=\"poptastic(this.href); return false;\">&nbsp&nbsp&nbsp view &nbsp&nbsp&nbsp</a></LI>");
        out.println("</td>");
        out.println("</ul>");
       

    }

    private void printInputs(PrintWriter out,Group group)
    {
        out.println("<td>"+group.getGroupName()+"</td>");
    }
    private void printForms(PrintWriter out,String groupname)
    {
        this.printDeleteForm(out,groupname);
        this.printChangeForm(out,groupname);
    }
    private void printDeleteForm(PrintWriter out,String groupname)
    {
        out.println("<FORM ACTION=/gsn/MyDeleteGroupServlet METHOD=POST>");
        out.println("<INPUT  TYPE=HIDDEN NAME=groupname VALUE="+groupname+">");
        out.println("<td><INPUT TYPE=SUBMIT  class=creategroupbuttonstyle VALUE=\"Delete Group\"></td>");
        out.println("</FORM>");
    }
    private void printChangeForm(PrintWriter out,String groupname)
    {
        out.println("<FORM ACTION=/gsn/MyChangeGroupCombinationServlet METHOD=GET>");
        out.println("<INPUT  TYPE=HIDDEN NAME=groupname VALUE="+groupname+">");
        out.println("<td><INPUT TYPE=SUBMIT  class=creategroupbuttonstyle VALUE=\"Change Group \"></td>");
        out.println("</FORM>");

    }



    
    /****************************************** Client Session related Methods*******************************************/
    /********************************************************************************************************************/

    private void setSessionPrintWriter(HttpServletRequest req,PrintWriter out)
    {
        req.getSession().setAttribute("out",out);
    }
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
            res.sendRedirect("https://"+req.getServerName()+"/gsn/MyGroupManagementServlet");

        }
    }
    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res)throws IOException
    {
        req.getSession().setAttribute("login.target", HttpUtils.getRequestURL(req).toString());
        res.sendRedirect("/gsn/MyLoginHandlerServlet");
    }

  



}
