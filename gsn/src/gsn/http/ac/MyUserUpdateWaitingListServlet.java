package gsn.http.ac;

import gsn.Main;
import gsn.beans.ContainerConfig;
import gsn.http.WebConstants;
import org.apache.log4j.Logger;


import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 26, 2010
 * Time: 2:34:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyUserUpdateWaitingListServlet  extends HttpServlet
{
      private static transient Logger logger                             = Logger.getLogger( MyUserUpdateWaitingListServlet.class );
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/


    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
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
               this.printHeader(out);
               this.printLayoutMastHead(out, user );
               this.printLayoutContent(out);
               try
               {
                   ctdb = new ConnectToDB();
                   Vector waitingUsers = ctdb.getWaitingUsers();
                   if(waitingUsers.size()==0)
                   {
                       out.println("<p><B> There is no entry in the waiting user list !</B> </p>");
                   }
                   for(int i=0;i<waitingUsers.size();i++)
                   {
                        //printForm(out,(User)(waitingUsers.get(i)));

                       this.printNewEntry(out,(User)(waitingUsers.get(i)));

                        
                   }
               }
               catch(Exception e)
               {
                   out.println("<p><B> Can not print the form</B> </p>");

                   logger.error("ERROR IN DOGET");
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
               //out.println("</BODY>");
               this.printLayoutFooter(out);
           }


       }
    }
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
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
		out.println("<TITLE>Users Update Waiting List</TITLE>");
        out.println(" <link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/style/acstyle.css\"/>");
        //printStyle(out);
        out.println("</HEAD>");
        out.println("<body onload=\"loadScroll()\" onunload=\"saveScroll()\" >");
        //call for alert pop up 
        //out.println("<input type=\"button\" onclick=\"popup()\" value=\"popup\">");
        out.println("<div id=\"container\">");
        out.println("<div class=box>");

	}
    private void printLayoutMastHead(PrintWriter out, User user)
    {
        out.println("<div id=\"masthead\">");
        out.println("<h1><a id=\"gsn-name\" style=\"\" href=\"/\">" + Main.getContainerConfig( ).getWebName( ) + "</a></h1>");

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
        out.println("<li class=\"selected\"><a href=/gsn/MyUserUpdateWaitingListServlet>user updates</a></li>");
    }
    private void printUserName(PrintWriter out, User user)
    {
        //String username=user.getUserName();
        out.println("<li><a href=\"/gsn/MyLogoutHandlerServlet\">logout</a></li>");
        out.println("<li><div id=\"logintextprime\">logged in as : "+user.getUserName()+"</div></li>");


    }
    
    private void printNewEntry(PrintWriter out,User user) throws ServletException
    {
        out.println("<h2>New Entry In Waiting List</h2>");
        //out.println("<BR>");

        //out.println("<h3>User Information</h3>");
        out.println("<br>");
        out.println("<li class=registerli >User Information </li><br>");

        this.printUserInformation(out,user);

        //out.println("<h3>Selected Groups </h3>");
        out.println("<br>");
        out.println("<li class=registerli >Selected Groups </li>");

        this.printUserGroupList(out,user);

        //out.println("<h3>Selected Virtual Sensors</h3>");
        out.println("<br>");

        out.println("<li class=registerli>Selected Virtual Sensors</li><br>");

        this.printUserDataSourceList(out,user);

        out.println("<BR>");


   }
    //new version without group ds combunation
    private void printUserGroupList(PrintWriter out,User user)
    {
        Group group=null;
        String label=null;
        String groupName=null;
        String groupType=null;
        String userName = user.getUserName();
        if(user.getGroupList().size()==0)
        {

            out.println("<p>No group is selected.</p>");
        }
        else
        {
            out.println("<table class=tab >");
            out.println("<tr><th> group name </th>");
            out.println("<th> group structure</th>");
            out.println("<th> user choice</th>");
            out.println("<th> admin decision</th>");
            out.println("<th> admin decision</th></tr>");
            for(int i=0; i<user.getGroupList().size();i++)
            {

                group=(Group)(user.getGroupList().get(i));
                groupName=group.getGroupName();
                groupType=group.getGroupType();

                if(groupType.equals("5"))
                {
                    label=" user wants to add this group ";
                }
                else if(groupType.equals("0"))
                {
                    label=" user wants to delete this group ";
                }
                out.println("<tr><td>"+groupName +"</td>");
                this.printGroupStructureLink(out, group.getGroupName());
                out.println("<td>"+label  +" </td>");


                out.println("<FORM ACTION=/gsn/MyUpdateUserGroupServlet METHOD=POST>");
                out.println("<INPUT  TYPE=HIDDEN NAME=groupname VALUE="+groupName+">");
                out.println("<INPUT  TYPE=HIDDEN NAME=grouptype VALUE="+groupType+">");
                out.println("<INPUT TYPE=HIDDEN NAME=username VALUE= "+userName+">");
                out.println("<INPUT TYPE=HIDDEN NAME=update VALUE= yes>");
                out.println("<td><INPUT TYPE=SUBMIT class=creategroupbuttonstyle VALUE=\"agree to update\"></td>");
                out.println("</FORM>");

                out.println("<FORM ACTION=/gsn/MyUpdateUserGroupServlet METHOD=POST>");
                out.println("<INPUT  TYPE=HIDDEN NAME=groupname VALUE="+groupName+">");
                out.println("<INPUT  TYPE=HIDDEN NAME=grouptype VALUE="+groupType+">");
                out.println("<INPUT TYPE=HIDDEN NAME=username VALUE= "+userName+">");
                out.println("<INPUT TYPE=HIDDEN NAME=update VALUE= no>");
                out.println("<td><INPUT TYPE=SUBMIT class=creategroupbuttonstyle VALUE=\"refuse to update\"></td></tr>");
                out.println("</FORM>");
            }
            out.println("</table>");

        }

    }
    private void printGroupStructureLink(PrintWriter out, String groupname)
    {
        String groupurl="/gsn/MyGroupHtmlResultSetServlet?groupname="+groupname;
        out.println("<ul class=displaylinkul >");
        out.println("<td style=text-align:center><LI class=displaylinkli><a href="+groupurl+" onClick=\"poptastic(this.href); return false;\">&nbsp&nbsp&nbsp view &nbsp&nbsp&nbsp</a></LI></td>");
        out.println("</ul>");

    }

     private void printGroupListModulo(PrintWriter out,User user,int index)
    {
        Group group=null;
        String label=null;
        String groupName=null;
        String groupType=null;
        String userName = user.getUserName();
        group=(Group)(user.getGroupList().get(index));
        groupName=group.getGroupName();
        groupType=group.getGroupType();
        if(groupType.equals("5"))
        {
            label=" user wants to add this group ";
        }
        else if(groupType.equals("0"))
        {
             label=" user wants to delete this group ";
        }

        out.println("<table class=\"transparenttable\">");
        out.println("<tr><td><B>groupname: </B>"+groupName +"</td></tr>");
        out.println("<tr><td><B>user choice: </B>"+label  +" </td></tr>");
        out.println("</table>");
        out.println("<BR>");
        this.printGroupDataSourceList(out,group);
        out.println("<BR>");

        out.println("<FORM ACTION=/gsn/MyUpdateUserGroupServlet METHOD=POST>");
        out.println("<INPUT  TYPE=HIDDEN NAME=groupname VALUE="+groupName+">");
        out.println("<INPUT  TYPE=HIDDEN NAME=grouptype VALUE="+groupType+">");
        out.println("<INPUT TYPE=HIDDEN NAME=username VALUE= "+userName+">");
        out.println("<INPUT TYPE=HIDDEN NAME=update VALUE= yes>");
        out.println("<table class=\"transparenttable\">");
        out.println("<td><INPUT TYPE=SUBMIT class=creategroupbuttonstyle VALUE=\"agree to update\"></td>");
        out.println("</FORM>");

        out.println("<FORM ACTION=/gsn/MyUpdateUserGroupServlet METHOD=POST>");
        out.println("<INPUT  TYPE=HIDDEN NAME=groupname VALUE="+groupName+">");
        out.println("<INPUT  TYPE=HIDDEN NAME=grouptype VALUE="+groupType+">");
        out.println("<INPUT TYPE=HIDDEN NAME=username VALUE= "+userName+">");
        out.println("<INPUT TYPE=HIDDEN NAME=update VALUE= no>");
        out.println("<td><INPUT TYPE=SUBMIT class=creategroupbuttonstyle VALUE=\"refuse to update\"></td></tr>");
        out.println("</FORM>");

        out.println("</table>");

    }


    private void printGroupDataSourceList(PrintWriter out,Group group)
    {
        DataSource ds=null;
        String dsname=null;
        String dstype=null;
        String label=null;
        out.println("<table class=tab >");
        out.println(" <caption>"+group.getGroupName()+" combination</caption>");
        out.println("<tr><th> virtual sensor name </th>");
        out.println("<th> access right</th></tr>");

        for(int j=0;j<group.getDataSourceList().size();j++)
        {
            ds=(DataSource)group.getDataSourceList().get(j);
            dsname=ds.getDataSourceName();
            dstype=ds.getDataSourceType();

            if(dstype.charAt(0)=='1')
            {
                label="read";
            }
            else if(dstype.charAt(0)=='2')
            {
                label="write";
            }
            else if(dstype.charAt(0)=='3')
            {
                label="read/write";
            }
            out.println("<tr><td>" +dsname+"</td>");
            out.println("<td>" +label  +"</td></tr>");
        }
        out.println("</table>");

    }

    private void printUserInformation(PrintWriter out,User user)
    {
        out.println("<table class=tab>");
        out.println("<tr><th>username</th>");
        out.println("<th>user first name</th>");
        out.println("<th>user last name</th>");
        out.println("<th>user E-mail</th></tr>");
        out.println("<tr><td>"+user.getUserName() +"</td>");
        out.println("<td>"+user.getFirstName()  +" </td>");
        out.println("<td>"+user.getLastName() +"</td>");
        out.println("<td>"+user.getEmail() +"</td></tr>");
        out.println("</table>");

    }
    private void printUserDataSourceList(PrintWriter out,User user)
    {
        String userName=user.getUserName();
        DataSource ds=null;
        String dsname=null;
        String dstype=null;
        String ownerDecision=null;
        String label=null;

         if(user.getDataSourceList().size()==0)
        {
            out.println("<p>No virtual sensor is selected.</p>");
            out.println("<BR>");
        }

        else
         {

            out.println("<table class=tab>");
            out.println("<tr><th> virtual sensor name </th>");
            out.println("<th> access right</th>");
            out.println("<th> owner decision</th>");
            out.println("<th> admin decision</th>");
            out.println("<th> admin decision</th></tr>");
            for(int i=0; i<user.getDataSourceList().size();i++)
            {

                ds=(DataSource)(user.getDataSourceList().get(i));

                dsname=ds.getDataSourceName();
                dstype=ds.getDataSourceType();
                ownerDecision=ds.getOwnerDecision();
               
                if(dstype.charAt(1)=='1')
                {
                    label="read";
                }
                else if(dstype.charAt(1)=='2')
                {
                    label="write";
                }
                else if(dstype.charAt(1)=='3')
                {
                    label="read/write";
                }
                else if(dstype.charAt(1)=='0')
                {
                    label="delete";
                }
                if(ownerDecision.equals("notreceived"))
                {
                     ownerDecision = "not received";
                }
                out.println("<tr><td>" +dsname+"</td>");
                out.println("<td>" +label  +"</td>");
                out.println("<td>" +ownerDecision  +"</td>");

                out.println("<FORM ACTION=/gsn/MyUpdateUserDataSourceServlet METHOD=POST>");
                out.println("<INPUT TYPE=HIDDEN NAME=username VALUE= "+userName+">");
                out.println("<INPUT TYPE=HIDDEN NAME= datasourcename VALUE= "+dsname+"> ");
                out.println("<INPUT TYPE=HIDDEN NAME= datasourcetype VALUE= "+dstype+"> ");
                out.println("<INPUT TYPE=HIDDEN NAME=update VALUE= yes>");
                out.println("<td><INPUT TYPE=SUBMIT class=creategroupbuttonstyle VALUE=\"agree to update \"></td>");
                out.println("</FORM>");

                out.println("<FORM ACTION=/gsn/MyUpdateUserDataSourceServlet METHOD=POST>");
                out.println("<INPUT TYPE=HIDDEN NAME=username VALUE= "+userName+">");
                out.println("<INPUT TYPE=HIDDEN NAME= datasourcename VALUE= "+dsname+"> ");
                out.println("<INPUT TYPE=HIDDEN NAME= datasourcetype VALUE= "+dstype+"> ");
                out.println("<INPUT TYPE=HIDDEN NAME=update VALUE= no>");
                out.println("<td><INPUT TYPE=SUBMIT class=creategroupbuttonstyle VALUE=\"refuse to update\"></td></tr>");
                out.println("</FORM>");
            }
         }
        out.println("</table>");

    }
    private void printFooter(PrintWriter out) throws ServletException
    {

        out.println("<p>\n" +
                "<table class=tab width=\"100%\"><tr>\n" +
                "<td align=right><A HREF=\"/gsn/MyLogoutHandlerServlet\">logout</a>"+
                "  <A HREF=/gsn/MyAdminManagementServlet>back to admin account management </a></td>"+
                "</tr></table>");
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
            res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyUserUpdateWaitingListServlet");

        }
    }
    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res)throws IOException
    {
        req.getSession().setAttribute("login.target", HttpUtils.getRequestURL(req).toString());
        res.sendRedirect("/gsn/MyLoginHandlerServlet");
    }





}
