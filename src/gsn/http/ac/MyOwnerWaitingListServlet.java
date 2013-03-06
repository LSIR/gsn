package gsn.http.ac;

import gsn.Main;
import gsn.beans.ContainerConfig;


import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 25, 2010
 * Time: 10:48:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class MyOwnerWaitingListServlet extends HttpServlet
{
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/

    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        // Get the session
        HttpSession session = req.getSession();
        ConnectToDB ctdb = null;

        User user = (User) session.getAttribute("user");
        if (user == null)
        {
        	UserUtils.redirectToLogin(req,res);
        }
        else
        {
            this.checkSessionScheme(req,res);
            //ConnectToDB ctdb = new ConnectToDB();
            this.printHeader(out);
            this.printLayoutMastHead(out, user );
            this.printLayoutContent(out);
            try
            {
                ctdb = new ConnectToDB();
                Vector datasourceNames=ctdb.getDataSourceNamesListForThisOwner(user);
                if(datasourceNames.size()==0)
                {
                    out.println("<p>There is no virtual sensor entry in the owner waiting list  ! </p>");
                }
                else
                {
                    int sizeOfPastusers=0;
                    for(int i=0;i<datasourceNames.size();i++)
                    {
                        Vector users = ctdb.completeUsersList( ctdb.getUsersWaitingForThisOwnerDecision((String)(datasourceNames.get(i))));

                        if(users.size()!=0)
                        {
                            sizeOfPastusers=users.size();
                        }
                        for(int j=0;j<users.size();j++)
                        {
                            this.printNewEntry(out,(User)users.get(j));
                        }
                        if(users.size()==0 && (i+1)==datasourceNames.size() && sizeOfPastusers==0)
                        {
                            out.println("<p> There is no user entry in the owner waiting list ! </p>");
                        }
                    }
                }
            }
            catch(Exception e)
            {
                out.println("Exception caught : "+e.getMessage());
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
        this.printLayoutFooter(out);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        handleForm(req, res);
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
		out.println("<TITLE>Owner Waiting List</TITLE>");
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

        out.println("<div class=\"image_float\"><img src=\"/style/gsn-mark.png\" alt=\"GSN logo\" /></div><br>");
        out.println("<h1>Owner Waiting List </h1>");
        out.println("<div class=\"spacer\"></div>");

        out.println("</div>");
        out.println("<div id=\"mastheadborder\">");
        this.printLinks(out);
        this.printUserName(out, user);
        out.println("<br><br>");
        out.println("</div>");
    }
    private void printLayoutContent(PrintWriter out)
    {
        out.println("<div id=\"content\">");
    }
    private void printLayoutFooter(PrintWriter out)
    {
        out.println("</div>");
        out.println("<div id=\"footer\">");
        out.println(" <p align=\"center\"><FONT COLOR=\"#000000\"/>Powered by <a class=\"nonedecolink\" href=\"http://globalsn.sourceforge.net/\">GSN</a>,  Distributed Information Systems Lab, EPFL 2010</p>");
        out.println("</div>");//footer
        out.println("</div>");//box
        out.println("</div>");//container
        out.println("</body>");
        out.println("</html>");
    }


     private void printNewEntry(PrintWriter out,User user) throws ServletException
     {
         out.println("<h2>New Entry In Waiting List</h2>");
         out.println("<div class=\"image_float\">");
         this.printUserInformation(out,user);
         out.println("</div>");
         this.printForms(out,user);
         out.println("<div class=\"spacer\"></div>");

    }

    private void printLinks(PrintWriter out)
    {
        //out.println("<a class=linkclass href=\"/gsn/MyLoginHandlerServlet\">login</a>");
        //out.println("<a class=linkclass href=/gsn/MyAccessRightsManagementServlet>access rights management</a>");
        out.println("<a class=linkclass href=\"/gsn/MyUserAccountManagementServlet\">User account</a>");
        out.println("<a class=linkclass href=\"/gsn/MyLogoutHandlerServlet\">logout</a>");
        //out.println("<a class=linkclass href=\"/\">GSN home</a>");
    }
    private void printUserName(PrintWriter out, User user)
    {
        //String username=user.getUserName();
        out.println("<p id=\"login\">logged in as : "+user.getUserName()+"</p>");


    }


    private void printForms(PrintWriter out,User user) throws ServletException
    {
        out.println("<B>&nbsp Do you agree that this candidate register for this virtual sensor? </B><br><br>");
        out.println("<table class=\"transparenttable\">");
        printYesForm(out, user);
        printNoForm(out, user);
        out.println("</table>");

     }
    private void printYesForm(PrintWriter out, User user)
    {
        String username=user.getUserName();
        String datasourcename= user.getDataSource().getDataSourceName();
        out.println("<FORM METHOD=POST>");
        out.println("<INPUT TYPE=HIDDEN NAME=username VALUE="+username+">");
        out.println("<INPUT TYPE=HIDDEN NAME=datasourcename VALUE="+datasourcename+">");
        out.println("<INPUT TYPE=HIDDEN NAME=register VALUE= Yes>");
        out.println("<tr><td><INPUT TYPE=SUBMIT class= buttonstyle VALUE=Yes></td>");
        out.println("</FORM>");

    }
    private void printNoForm(PrintWriter out, User user)
    {
        String username=user.getUserName();
        String datasourcename= user.getDataSource().getDataSourceName();
        out.println("<FORM METHOD=POST>");
        out.println("<INPUT TYPE=HIDDEN NAME=username VALUE="+username+">");
        out.println("<INPUT TYPE=HIDDEN NAME=datasourcename VALUE="+datasourcename+">");
        out.println("<INPUT TYPE=HIDDEN NAME=register VALUE= No>");
        out.println("<td><INPUT TYPE=SUBMIT class= buttonstyle VALUE=\"No\"></td></tr>");
        out.println("</FORM>");
    }
    private void printUserInformation(PrintWriter out,User user)
    {
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>first name</th>");
        out.println("<td>"+ user.getFirstName() +"</td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<th>last name</th>");
        out.println("<td>"+ user.getLastName() +"</td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<th>E-mail</th>");
        out.println("<td>"+ user.getEmail() +"</td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<th>virtual sensor name</th>");
        out.println("<td>"+ user.getDataSource().getDataSourceName()  +"</td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<th>access right</th>");
        out.println("<td>"+ setLabel(user.getDataSource())  +"</td>");
        out.println("</tr>");
        out.println("</table>");
        

    }


    private String setLabel(DataSource ds)
    {
        String label=null;
        if(ds.getDataSourceType().charAt(1)=='1')
        {
                label="read";
        }
        else if(ds.getDataSourceType().charAt(1)=='2')
        {
            label="write";
        }
        else if(ds.getDataSourceType().charAt(1)=='3')
        {
            label="read/write";
        }
        return label;

    }
    /****************************************** AC Related Methods*******************************************/
    /***********************************************************************************************************/

    void handleForm(HttpServletRequest req, HttpServletResponse res)
    {
        User temp=null;
        HttpSession session = req.getSession();
        PrintWriter out = (PrintWriter) session.getAttribute("out");

        ParameterSet pm = new ParameterSet(req);

        ConnectToDB ctdb =null;
        try
        {
            ctdb= new ConnectToDB();
            if(pm.valueForName("register")==null)
            {
                return;
            }

            else if(pm.valueForName("register").equals("Yes"))
            {

                ctdb.updateOwnerDecision("has accepted the registration",pm.valueForName("username"), pm.valueForName("datasourcename") );
            }

            else if(pm.valueForName("register").equals("No"))
            {
                ctdb.updateOwnerDecision("has refused the registration",pm.valueForName("username"), pm.valueForName("datasourcename") );

            }
        }
        catch(Exception e)
        {
            out.println("Exception caught : "+e.getMessage());
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
            res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyOwnerWaitingListServlet");

        }
    }


}
