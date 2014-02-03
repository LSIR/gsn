package gsn.http.ac;

import gsn.Main;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 24, 2010
 * Time: 3:17:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyUpdateUserWaitingForGroupServlet  extends HttpServlet
{
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        // Get the session
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        ConnectToDB ctdb = null;
        if (user == null)
       {
           this.redirectToLogin(req,res);
       }
        else
       {
           this.checkSessionScheme(req,res);
           ParameterSet pm = new ParameterSet(req);
           if(pm.valueForName("groupname")==null)
           {
               res.sendRedirect("/");
               return;
           }
           else if(pm.valueForName("groupname").equals(""))
           {
               res.sendRedirect("/");
               return;
           }
           else
           {
               try
               {
                   ctdb = new ConnectToDB();
                   if(ctdb.valueExistsForThisColumnUnderTwoConditions(new Column("ISUSERWAITING","yes"),new Column("GROUPNAME",pm.valueForName("groupname")),new Column("USERNAME",user.getUserName()),"ACUSER_ACGROUP")==false)
                   {
                       Group group=new Group(pm.valueForName("groupname"));
                       if(pm.valueForName("addgroup")!=null)
                       {
                           if(pm.valueForName("addgroup").equals("Yes"))
                           {
                                group.setGroupType("5");
                                user.setIsWaiting("yes");
                                ctdb.registerGroupForUser(user,group);

	       						Emailer email = new Emailer();
	       		                String msgHead = "Dear GSN Admin"+"\n"+"\n";
	
	       						String msgBody = user.getFirstName()+" "+user.getLastName()+" ("+user.getUserName()+") would like to be added to group "+group.getGroupName()+".\n"+"\n";
	       						
	       						String msgTail = "Best Regards,"+"\n"+"GSN Team";
	       						
	       						// first change Emailer class params to use sendEmail
	                            email.sendEmail(ctdb.getUserForUserName("Admin").getEmail(),Main.getContainerConfig( ).getWebName( )+": Add to Group request", msgHead, msgBody, msgTail);
	                       }
                         }
                         if(pm.valueForName("deletegroup")!=null)
                         {
                             if(pm.valueForName("deletegroup").equals("Yes"))
                             {
                                 group.setGroupType("0");
                                 user.setIsWaiting("yes");
                                 ctdb.updateGroupForUser(user,group);

 	       						 Emailer email = new Emailer();
 	       		                 String msgHead = "Dear GSN Admin"+"\n"+"\n";
 	
 	       						 String msgBody = user.getFirstName()+" "+user.getLastName()+" ("+user.getUserName()+") would like to be removed from group "+group.getGroupName()+".\n"+"\n";
 	       						
 	       						 String msgTail = "Best Regards,"+"\n"+"GSN Team";
 	       						
 	       						 // first change Emailer class params to use sendEmail
 	                             email.sendEmail(ctdb.getUserForUserName("Admin").getEmail(),Main.getContainerConfig( ).getWebName( )+": Remove from Group request", msgHead, msgBody, msgTail);
 	                         }
                         }
                     }
                   res.sendRedirect("/gsn/MyUserUpdateServlet");
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
    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
	{
           this.doPost(req,res);
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
            res.sendRedirect("https://"+req.getServerName()+"/gsn/MyUpdateUserWaitingForGroupServlet");

        }
    }
    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res)throws IOException
    {
        req.getSession().setAttribute("login.target", HttpUtils.getRequestURL(req).toString());
        res.sendRedirect("/gsn/MyLoginHandlerServlet");
    }

}
