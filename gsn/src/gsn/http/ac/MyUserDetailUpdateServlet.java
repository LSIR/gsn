package gsn.http.ac;

import gsn.Main;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

public class MyUserDetailUpdateServlet extends HttpServlet
{
    private static transient Logger logger                             = Logger.getLogger( MyUserDetailUpdateServlet.class );
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/
    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null)
        {
           this.redirectToLogin(req,res);
        }
        else {
            res.setContentType("text/html");
            PrintWriter out = res.getWriter();
            checkSessionScheme(req, res);
            setSessionPrintWriter(req,out);
		    printHeader(out);
            printLayoutMastHead(out,user);
            printLayoutContent(out);
		    printForm(out, user);
		    printLayoutFooter(out);
        }

    }
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
        doGet(req,res);
        handleForm(req, res);
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
        out.println("<li><a href=/gsn/MyUserAccountManagementServlet>user account</a></li>");
        out.println("<li class=\"selected\"><a href=/gsn/MyUserDetailUpdateServlet>user details</a></li>");
    }
    
    private void printUserName(PrintWriter out, User user)
    {
        //String username=user.getUserName();
        out.println("<li><a href=\"/gsn/MyLogoutHandlerServlet\">logout</a></li>");
        out.println("<li><div id=\"logintextprime\">logged in as: "+user.getUserName()+"</div></li>");
    }
    
    private void printForm(PrintWriter out, User user) throws ServletException
	{
        Vector groupList = this.getGroupList();
        if(groupList==null)
        {
            out.println("<p><b>Can not print the form !</b></p>");
            return;
        }
        else
        {   out.println("<br>");
            out.println("<FORM METHOD=POST>");  // posts to itself

            //out.println("<div class=image_float>");
            out.println("<h2> Personal Information</h2>");
            //out.println("<br>");
            this.printPersonalInputs(out, user);
            out.println("<br>");
            out.println("<br>");
            //out.println("</div>");
            out.println("<h2> Account Information</h2>");
            //out.println("<font class=myhead> Account Information</font>");
            //out.println("<br>");
            this.printAccountInputs(out, user);
            out.println("<br>");
            out.println("<BR>");
            //out.println("<h2> Choose your group(s)</h2>");
            //out.println("<br>");
            //out.println("<font class=myhead> Choose your group(s)</font>");
            //this.printGroupList(out,groupList);

            out.println("<BR>");
            out.println("<BR>");
            this.printFormButtons(out);
            out.println("</FORM>");

        }

    }
    private void printPersonalInputs(PrintWriter out, User user)
    {
        out.println("<table class=tab>");
        out.println("<tr><th>first name</th><td><input class=\"inputclass\" type=\"text\" name=\"firstname\" size=\"30\" value=\"" + user.getFirstName() + "\" /></td></tr>");
        out.println("<tr><th>last name</th><td><input class=\"inputclass\" type=\"text\" name=\"lastname\" size=\"30\" value=\"" + user.getLastName() + "\" /></td></tr>");
        out.println("<tr><th>E-mail</th><td><input class=\"inputclass\" type=\"text\" name=\"email\"  size=\"30\" value=\"" + user.getEmail() + "\"/></td></tr>");

        out.println("</table>");

    }
    private void printAccountInputs(PrintWriter out, User user)
    {
        out.println("<table class=tab>");
        out.println("<tr><th>username</th><td>" + user.getUserName() + "</td></tr>");
        out.println("<tr><th>password</th><td><input class=\"inputclass\" type=\"password\" name=\"password\" size=\"30\" /></td></tr>");
        out.println("<tr><th>new password</th><td><input class=\"inputclass\" type=\"password\" name=\"newpassword\" size=\"30\" /></td></tr>");
        out.println("</table>");

    }

    private void printFormButtons(PrintWriter out)
    {
        //out.println("<table class=transparenttable>");
        out.println("<INPUT TYPE=SUBMIT class=bigsumitbuttonstyle VALUE=\"Submit \">");
        //out.println("<td><INPUT TYPE=RESET class=changegroupbuttonstyle VALUE=\"Reset\"></td></tr>");
        out.println("</table>");
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
           res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyUserCandidateRegistrationServlet");

       }
   }
    /****************************************** DB related Methods******************************************************/
    /********************************************************************************************************************/

     private Vector getGroupList()
    {
        Vector groupList =null;
		ConnectToDB ctdb = null;
		try
		{   ctdb = new ConnectToDB();
			groupList = ctdb.getGroupList();
		}
        catch(Exception e)
        {

            logger.error("ERROR IN getGroupList");
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
        return groupList;
    }

    /****************************************** AC related Methods******************************************************/
    /********************************************************************************************************************/

    private void handleForm(HttpServletRequest req,HttpServletResponse res) throws IOException
    {
        HttpSession session = req.getSession();
        PrintWriter out = (PrintWriter) session.getAttribute("out");
        ParameterSet pm = new ParameterSet(req);
        if (session.getAttribute("user") != null)
        {
	        User muser=allowUserToRegister(pm, out,new User((User)session.getAttribute("user")));
	        if(muser!= null)
	        {
	            try
				{
					res.sendRedirect("/gsn/MyLogoutHandlerServlet");
				}
				catch (Exception ignored)
				{
					out.println("problem with redirecting to the target !");
				}

	            ConnectToDB ctdb =null;
	            try {
	            	ctdb=new ConnectToDB();
	            	Emailer email = new Emailer();
	                String msgHead = "Dear GSN Admin, "+"\n"+"\n";
	                
	                String msgBody = muser.getFirstName() +" " + muser.getLastName() + "(username=" +muser.getUserName()+") has updated the user details:"+"\n"
	                        +"first name : "+muser.getFirstName()+"\n"
	                        +"last name : "+muser.getLastName()+"\n"
	                        +"email : "+muser.getEmail()+"\n"+"\n";
	                
	                String msgTail = "Best Regards,"+"\n"+"GSN Team";
	
	                // first change Emailer class params to use sendEmail
	                email.sendEmail(ctdb.getUserForUserName("Admin").getEmail(),Main.getContainerConfig( ).getWebName( )+": User detail update", msgHead, msgBody, msgTail);

	                msgHead = "Dear " + muser.getFirstName() +" " + muser.getLastName() +", "+"\n"+"\n";
	                
	                msgBody = "You have successfully updated your user details:"+"\n"
	                        +"first name : "+muser.getFirstName()+"\n"
	                        +"last name : "+muser.getLastName()+"\n"
	                        +"email : "+muser.getEmail()+"\n"+"\n";
	
	                // first change Emailer class params to use sendEmail
	                email.sendEmail(muser.getEmail(),Main.getContainerConfig( ).getWebName( )+": User detail updated", msgHead, msgBody, msgTail);
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
        }
    }

    private boolean isNotDefined (ParameterSet pm, String name) {
        return pm.valueForName(name) == null || "".equals(pm.valueForName(name));
    }

    User allowUserToRegister(ParameterSet pm,PrintWriter out,User user)
    {
		//User waitinguser=null;
		ConnectToDB ctdb =null;
        EmailAddress emailadd=null;
        try
		{
			if(isNotDefined(pm,"password") || isNotDefined(pm,"firstname") || isNotDefined(pm,"lastname") || isNotDefined(pm,"email"))
			{
				//out.println("At least one of the input parameters is empty "+"<br>");
                user = null;
                this.managaeUserAlert(out, "At least one of the input parameters is empty " );
			}
			else
			{
                emailadd= new EmailAddress(pm.valueForName("email"));
                if (emailadd.isValid()==false)
	            {
	                //out.println("Invalid email address "+"<br>");
                    this.managaeUserAlert(out, "Invalid email address " );
	                //redirect
	               // return false;
	            }
                else
                {
                    ctdb =new ConnectToDB();
	                if(ctdb.valueExistsForThisColumn(new Column("USERNAME",user.getUserName()), "ACUSER"))
                    {
                        String pwd = Protector.encrypt(pm.valueForName("password"));
                        if(ctdb.isPasswordCorrectForThisUser(user.getUserName(), pwd)) // Check if the current password matchs
                        {
                            String newpwd = isNotDefined(pm, "newpassword") ? pwd : Protector.encrypt(pm.valueForName("newpassword"));
                            user.setPassword(newpwd);
                            user.setFirstName(pm.valueForName("firstname"));
                            user.setLastName(pm.valueForName("lastname"));
                            user.setEmail(pm.valueForName("email"));
                            if(ctdb.updateUserDetails(user))
                            {
                                logger.debug("Successfully updated the user details.");
                            }
                            else
                            {
                                user = null;
                                this.managaeUserAlert(out, "User Detail Update failed !" );
                            }
                        }
                        else
                        {
                            user = null;
                            this.managaeUserAlert(out, "The password does not match the current password." );
                        }
                    }
                    else
                    {
                        user = null;
                        this.managaeUserAlert(out, "This username does not exist and thus can't be updated." );
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

		return user;
	 }

    
    private void managaeUserAlert(PrintWriter out, String alertMessage)
    {
        this.createAlertBox(out, alertMessage);
        this.callAlertBox(out);
    }


    private void createAlertBox(PrintWriter out, String alertMessage)
    {
        out.println("<div id=\"AlertBox\" class=\"alert\">");
        out.println("<p>");
        out.println(alertMessage );
        out.println("</p>");
        //out.println("<p>");
        //out.println("Failed to sign up, ");
        //out.println("you may want to try again !");
        //out.println("</p>");
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

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res)throws IOException
    {
        req.getSession().setAttribute("login.target", HttpUtils.getRequestURL(req).toString());
        res.sendRedirect("/gsn/MyLoginHandlerServlet");
    }


}