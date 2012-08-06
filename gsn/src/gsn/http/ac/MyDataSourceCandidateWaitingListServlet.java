package gsn.http.ac;

import gsn.Main;
import gsn.http.WebConstants;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 21, 2010
 * Time: 8:54:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyDataSourceCandidateWaitingListServlet extends HttpServlet
{
    private static transient Logger logger                             = Logger.getLogger( MyDataSourceCandidateWaitingListServlet.class );
    /****************************************** Servlet Methods*******************************************/
    /******************************************************************************************************/

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
                    Vector v=ctdb.getDataSourceCandidates();
                    if(v.size()==0)
                    {
                        out.println("<p><B>There is no entry in the waiting list !</p></B>");
                     }
                     for(int i=0;i<v.size();i++)
                    {
                        //printForm(out,(DataSource)(v.get(i)));
                        printNewEntry(out,(DataSource)(v.get(i)));
                    }
                }
                catch(Exception e)
                {
                    out.println("<p><B>Can not print the form !</p></B>");
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
               this.printLayoutFooter(out);
           }

       }

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
		out.println("<TITLE>Virtual Sensor Registration Waiting List</TITLE>");
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
        out.println("<li class=\"selected\"><a href=/gsn/MyDataSourceCandidateWaitingListServlet>virtual sensor requests</a></li>");
    }
    private void printUserName(PrintWriter out, User user)
    {
        //String username=user.getUserName();
        out.println("<li><a href=\"/gsn/MyLogoutHandlerServlet\">logout</a></li>");
        out.println("<li><div id=\"logintextprime\">logged in as : "+user.getUserName()+"</div></li>");


    }
    private void printNewEntry(PrintWriter out,DataSource datasource) throws ServletException
    {
        out.println("<h2>New Entry In Waiting List</h2>");
        out.println("<BR>");

        //out.println("<h3>Virtual Sensor Information</h3>");
        out.println("<li class=registerli >Virtual Sensor Information </li><br>");
        this.printDataSourceInformation(out,datasource);

        this.printForm(out,datasource);
        out.println("<br>");


   }
    private void printForm(PrintWriter out,DataSource datasource) throws ServletException
	{
        out.println("<FORM METHOD=POST>");  // posts to itself
        this.printFormInputs(out,datasource);
        //out.println("<h3>Admission Decision </h3>");
        out.println("<BR>");
        out.println("<li class=registerli >Admission Decision </li><br>");
        this.printAdmissionPart(out);
        out.println("<BR>");
        out.println("<BR>");
        this.printFormButtons(out);
		out.println("</FORM>");
		
    }
    private void printFormInputs(PrintWriter out,DataSource datasource)
    {
        String datasourcename=datasource.getDataSourceName();
	    out.println("<INPUT TYPE=HIDDEN NAME=datasourcename size=30 VALUE="+datasourcename +">");
    }
    private void printDataSourceInformation(PrintWriter out,DataSource datasource)
    {
        out.println("<table class=tab>");
        out.println("<tr><th>virtual sensor name</th>");
	    out.println("<th>file name</th>");
        out.println("<th>file type</th>");
	    out.println("<th>stored in</th>");
        out.println("<tr><td>"+datasource.getDataSourceName()+"</td>");
        out.println("<td>"+datasource.getFileName()+"</td>");
        out.println("<td>"+datasource.getFileType()+"</td>");
        out.println("<td>"+datasource.getPath() +"</td>");
        out.println("</table>");
        out.println("<br>");
        out.println("<li class=registerli >Virtual Sensor Owner Information</li><br>");
        //out.println("<h3>Virtual Sensor Owner Information</h3>");
        out.println("<table class=tab>");
        out.println("<tr><th>owner first name</th>");
        out.println("<th>owner last name</th>");
        out.println("<th>owner E-mail</th></tr>");
        out.println("<tr><td>"+datasource.getOwner().getFirstName() +"</td>");
        out.println("<td>"+datasource.getOwner().getLastName() +"</td>");
        out.println("<td>"+datasource.getOwner().getEmail() +"</td></tr>");


        out.println("</table>");

    }


    private void printAdmissionPart(PrintWriter out)
    {
        out.println("<table class=tab>");
	    out.println("<tr><th>Do you allow this virtual sensor registration?</th></tr>");
		//out.println("<tr><td><INPUT  TYPE=RADIO NAME=register VALUE= Yes><FONT COLOR=#000000> Yes ");
        out.println("<tr><td><select name=register id=selectbox>");
        out.println("<option value= >Select</option>");
        out.println(" <option value=Yes>Yes</option>");
        out.println(" <option value=No >No</option>");
        out.println(" </select></td></tr>");
        out.println("</table>");
        out.println("<BR>");
        out.println("<table class=tab >");
        out.println("<tr><th>If No, explain the reason here</th></tr> ");
        out.println("<tr><td><TEXTAREA NAME=comments COLS=40 ROWS=6></TEXTAREA></td></tr>");
        out.println("</table>");

    }
    private void printFormButtons(PrintWriter out)
    {
        //out.println("<table class=transparenttable>");
        out.println("<INPUT TYPE=SUBMIT class=sumitbuttonstyle VALUE=\"Submit \">");
        out.println("<INPUT TYPE=RESET class=sumitbuttonstyle VALUE=\"Reset\">");
        //out.println("</table>");

    }


    /****************************************** AC Related Methods*****************************************************/
    /******************************************************************************************************************/
    void handleForm(HttpServletRequest req, HttpServletResponse res)
    {
        HttpSession session = req.getSession();
		PrintWriter out = (PrintWriter) session.getAttribute("out");
		ParameterSet pm = new ParameterSet(req);
        String comments="";
        ConnectToDB ctdb = null;

        if(pm.valueForName("register")==null || pm.valueForName("datasourcename")==null ||pm.valueForName("datasourcename").equals(""))
        {
           return ;
        }
        else
        {
            if(pm.valueForName("register").equals("Yes")|| pm.valueForName("register").equals("No"))
            {
                try
                {
                    ctdb = new ConnectToDB();
                    if(pm.valueForName("register").equals("Yes"))
                    {
                        ctdb.updateOneColumnUnderOneCondition(new Column("ISCANDIDATE","no"),new Column("DATASOURCENAME",pm.valueForName("datasourcename")),"ACDATASOURCE");
                        //send e-mail to the DS owner
                    }
                    else if(pm.valueForName("register").equals("No"))
                    {
                        ctdb.deleteDataSourceCandidate(pm.valueForName("datasourcename"));
                        comments=pm.valueForName("comments");
                        //send e-mail to the DS owner
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
            else
            {
                return;
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
            res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyDataSourceCandidateWaitingListServlet");

        }
    }
    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res)throws IOException
    {
        req.getSession().setAttribute("login.target", HttpUtils.getRequestURL(req).toString());
        res.sendRedirect("/gsn/MyLoginHandlerServlet");
    }



}
