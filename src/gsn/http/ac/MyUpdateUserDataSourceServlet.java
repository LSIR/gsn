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

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 26, 2010
 * Time: 7:37:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyUpdateUserDataSourceServlet  extends HttpServlet
{
    private static transient Logger logger                             = Logger.getLogger( MyUpdateUserDataSourceServlet.class );
    /****************************************** Servlet Methods*******************************************/
    /******************************************************************************************************/
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
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
           if(!user.getUserName().equals("Admin"))
           {
               res.sendError( WebConstants.ACCESS_DENIED , "Access denied." );
           }
           else
           {
               ParameterSet pm = new ParameterSet(req);
               if(pm.valueForName("datasourcename")==null|| pm.valueForName("datasourcetype")==null|| pm.valueForName("update")==null || pm.valueForName("username")==null )
               {
                   res.sendRedirect("/");
                   return;
               }
               if(pm.valueForName("datasourcename").equals("")|| pm.valueForName("datasourcetype").equals("")|| pm.valueForName("update").equals("")|| pm.valueForName("username").equals("") )
               {
                   res.sendRedirect("/");
                   return;
               }
               try
               {
                   ctdb = new ConnectToDB();
                   User waitingUser = new User(pm.valueForName("username"));
                   //String updatedType=null;

                   if(pm.valueForName("update").equals("yes"))
                   {
                       if(pm.valueForName("datasourcetype").charAt(1)=='0')
                       {
                          ctdb.deleteDataSourceForUser(new DataSource(pm.valueForName("datasourcename")), waitingUser);
                       }
                       else
                       {
                           //updatedType=pm.valueForName("datasourcetype").substring(1,2);
                           waitingUser.setIsWaiting("no");
                           ctdb.updateDataSourceForUser(waitingUser,new DataSource(pm.valueForName("datasourcename"),pm.valueForName("datasourcetype").substring(1,2)));
                           ctdb.updateOwnerDecision("notreceived",pm.valueForName("username"), pm.valueForName("datasourcename") );
                       }
                   }
                   else if(pm.valueForName("update").equals("no"))
                   {
                       if(pm.valueForName("datasourcetype").charAt(0)=='5')
                       {
                           ctdb.deleteDataSourceForUser(new DataSource(pm.valueForName("datasourcename")), waitingUser);
                       }
                       else
                       {
                            //updatedType=pm.valueForName("datasourcetype").substring(1,2);
                           waitingUser.setIsWaiting("no");
                           ctdb.updateDataSourceForUser(waitingUser,new DataSource(pm.valueForName("datasourcename"),pm.valueForName("datasourcetype").substring(0,1)));
                           ctdb.updateOwnerDecision("notreceived",pm.valueForName("username"), pm.valueForName("datasourcename") );
                       }
                   }




                    res.sendRedirect("/gsn/MyUserUpdateWaitingListServlet");
                }
                catch(Exception e)
                {
                    logger.error("ERROR IN doPost");
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
            res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyUpdateUserDataSourceServlet");

        }
    }


}
