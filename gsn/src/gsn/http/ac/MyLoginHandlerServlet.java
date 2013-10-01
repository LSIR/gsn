package gsn.http.ac;

import gsn.Main;
import org.apache.log4j.Logger;

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
 * Date: Apr 14, 2010
 * Time: 1:51:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyLoginHandlerServlet extends HttpServlet
{
        private static transient Logger logger                             = Logger.getLogger( MyLoginHandlerServlet.class );
      /****************************************** Servlet Methods*******************************************/
    /******************************************************************************************************/
    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{

        res.setContentType("text/html");
		PrintWriter out = res.getWriter();
        checkSessionScheme(req,res);
        setSessionPrintWriter(req,out);
        this.printHeader(out);
        this.printLayoutMastHead(out);
        this.printLayoutContent(out);
		this.printLayoutForm(out);
        this.printLayoutFooter(out);
    }
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
        doGet(req,res);
        handleForm(req, res);

    }
     /****************************************** HTML Printing Methods*******************************************/
    /***********************************************************************************************************/

    private void printHeader(PrintWriter out) throws ServletException
	{
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<script type=\"text/javascript\" src=\"/js/acjavascript.js\"></script>");
		out.println("<TITLE>Login Form</TITLE>");
        out.println(" <link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/style/acstyle.css\"/>");
        out.println("</HEAD>");
        out.println("<body>");
        out.println("<div id=\"container\">");
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

    private void printLayoutForm(PrintWriter out)
    {
        out.println("<div id=\"loginhandlercontainer\">");
        out.println(" <form method=\"post\" id=\"enquiryform\">");

        //out.println("<div class=image_float>");
        out.println("<h2>Login Form</h2>");
        //out.println("<br>");
        this.printFormInputs(out);
        out.println("<br>");
        out.println("<br>");


        out.println("<input type=\"submit\" class=\"loginhandlerbuttonstyle\" value=\"Login\" tabindex=\"3\" />");


        out.println("</form>");
        out.println("</div>");
        out.println("<HR>");
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
        out.println("</div>");//container
        out.println("</body>");
        out.println("</html>");
    }

    private void printFormInputs(PrintWriter out)
    {
        out.println("<table class=tab>");
        out.println("<tr><th>username</th><td><INPUT class=inputclass TYPE=TEXT name=\"username\" id=\"loginhandlerusername\" tabindex=\"1\"size=30></td></tr>");
        out.println("<tr><th>password</th><td><INPUT class=inputclass type=\"PASSWORD\" name=\"password\" id=\"loginhandlerpassword\" tabindex=\"2\" size=30></td></tr>");
        out.println("</table>");
    }

    private void printLinks(PrintWriter out)
    {
        out.println("<li><a href=\"/\">Home</a></li>");
        out.println("<li class=\"selected\" style=\"font-size:0.8em;font-weight:bolder;padding: 0 8px;text-decoration:none;text-transform:uppercase;\">login</li>");

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
            res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyLoginHandlerServlet");

        }
    }

     /****************************************** AC related Methods*******************************************************/
    /********************************************************************************************************************/
    private void handleForm(HttpServletRequest req,HttpServletResponse res) throws IOException
    {
        HttpSession session = req.getSession();
		PrintWriter out = (PrintWriter) session.getAttribute("out");
		ParameterSet pm = new ParameterSet(req);
		User user = allowUserToLogin(out,pm, req);
		if(user!= null)
		{
            session.setAttribute("user",user);  // just a marker object
            if(req.getHeader("client")!=null)
            {
                if(req.getHeader("client").equals("apache"))
                {
                    res.setHeader("logedin","yes");
                    return;
                }
            }
			// Try redirecting the client to the page he first tried to access
			try
			{
				String target = (String) session.getAttribute("login.target");
                out.println("target : "+target);
                if (target != null)
                {
                    res.sendRedirect(target);
					return;
                }
                else//if target is null, redirect to home
                {
                    if(session.getAttribute("scheme").equals("http"))
                    {
                        res.sendRedirect("http://"+req.getServerName()+":"+ Main.getContainerConfig().getContainerPort()+"/");
                    }
                    else if(session.getAttribute("scheme").equals("https"))
                    {
                        res.sendRedirect("/");
                    }
				}
			}
			catch (Exception ignored)
			{
				out.println("problem with loggin target : ");
                out.println(ignored.getMessage());
                out.println(ignored.getCause());
			}
		}
		else
		{
             if(req.getHeader("client")!=null)
            {
                if(req.getHeader("client").equals("apache"))
                {
                    res.setHeader("logedin","no");
                     return;
                }
            }

		}

	}

    User allowUserToLogin(PrintWriter out,ParameterSet pm,HttpServletRequest req)
    {
        User user= null;
        ConnectToDB ctdb = null;

        try
        {
            if(pm.hasEmptyParameter())
            {

                this.managaeUserAlert(req,out,"At least one of the input parameters is empty !");


            }
            else
            {
                ctdb = new ConnectToDB();
                if(ctdb.valueExistsForThisColumnUnderOneCondition(new Column("USERNAME",pm.valueForName("username")),new Column("ISCANDIDATE","no"),"ACUSER")==true)
                {
                    String enc= Protector.encrypt(pm.valueForName("password"));
                    if((ctdb.isPasswordCorrectForThisUser(pm.valueForName("username"),enc)== false))
                    {

                        this.managaeUserAlert(req,out, "Incorrect password !" );
                    }
                    else
                    {
                        out.println("You are allowed to see the target!"+"<br>");
                        user = new User(pm.valueForName("username"),enc,ctdb.getDataSourceListForUserLogin(pm.valueForName("username")),ctdb.getGroupListForUser(pm.valueForName("username")));
                        User userFromBD = ctdb.getUserForUserName(pm.valueForName("username"));
                        user.setLastName(userFromBD.getLastName());
                        user.setEmail(userFromBD.getEmail());
                        user.setFirstName(userFromBD.getFirstName());
                    }

                }
                else
                {

                    this.managaeUserAlert(req,out, "This username does not exist !"  );

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
        return user;
    }

    private void managaeUserAlert(HttpServletRequest req, PrintWriter out, String alertMessage)
    {
        String finalAlertMessage=null;
        if(req.getHeader("client")==null)
        {
            this.createAlertBox(out, alertMessage);
            this.callAlertBox(out);
        }
        else
        {
             if(req.getHeader("client").equals("apache"))
            {
               out.println(alertMessage);
            }

        }

    }

    private void createAlertBox(PrintWriter out, String alertMessage)
    {
        out.println("<div id=\"AlertBox\" class=\"alert\">");
        out.println("<p>");
        out.println(alertMessage );
        out.println("</p>");
        out.println("<p>");
        out.println("Failed to log in, ");
        out.println("you may want to try again !");
        out.println("</p>");
        out.println("<form style=\"text-align:right\">");
        out.println("<input");
        out.println("type=\"button\"");
        out.println("class= alertbuttonstyle");
        out.println("value=\"OK\"");
        out.println("style=\"width:75px;\"");
        out.println("onclick=\"document.getElementById('AlertBox').style.display='none'\">");
        out.println("</form>");
        out.println("</div>");

    }
    private void callAlertBox(PrintWriter out)
    {
        out.println("<SCRIPT LANGUAGE=\"JavaScript\" TYPE=\"TEXT/JAVASCRIPT\">");
        out.println("<!--");
        out.println("DisplayAlert('AlertBox',500,200);");
        out.println("//-->");
        out.println("</SCRIPT>");
    }



}
