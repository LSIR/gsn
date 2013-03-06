package gsn.http.ac;

import gsn.Main;
import gsn.beans.ContainerConfig;


import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 24, 2010
 * Time: 4:41:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyUpdateUserWaitingForDataSourceServlet extends HttpServlet
{
    /****************************************** Servlet Methods*******************************************/
    /****************************************************************************************************/
    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        // Get the session
        HttpSession session = req.getSession();
        ConnectToDB ctdb = null;
        DataSource newDataSource=null;
        DataSource oldDataSource=null;
        //String newType=null;
        User user = (User) session.getAttribute("user");
        if (user == null)
       {
        	UserUtils.redirectToLogin(req,res);
       }
       else
       {
           this.checkSessionScheme(req,res);
           ParameterSet pm = new ParameterSet(req);

           try
           {
               ctdb=new ConnectToDB();
               if(ctdb.getDataSourceListForParameterSet(pm)==null)
               {
                   res.sendRedirect("/");
                   return;
               }
               if(ctdb.getDataSourceListForParameterSet(pm).size()==0)
               {
                   res.sendRedirect("/gsn/MyUserUpdateServlet");
                   return;
               }
               newDataSource=(DataSource)ctdb.getDataSourceListForParameterSet(pm).get(0);
               if(newDataSource==null)
               {
                   res.sendRedirect("/");
                   return;
               }

               oldDataSource=ctdb.getDataSourceForUser(user,newDataSource.getDataSourceName());


               if(oldDataSource !=null)
               {


               }
               if(ctdb.valueExistsForThisColumnUnderTwoConditions(new Column("ISUSERWAITING","yes"),new Column("USERNAME",user.getUserName()),new Column("DATASOURCENAME",newDataSource.getDataSourceName()), "ACUSER_ACDATASOURCE")==false)
               {

                   if(oldDataSource==null)
                   {
                       
                       user.setIsWaiting("yes");
                       newDataSource.setDataSourceType("5"+ newDataSource.getDataSourceType());
                       newDataSource.setOwnerDecision("notreceived");
                       ctdb.registerDataSourceForUser(user,newDataSource);
                   }
                   else
                   {
                       if(oldDataSource.getDataSourceType().equals(newDataSource.getDataSourceType())==false)
                       {
                           user.setIsWaiting("yes");
                           oldDataSource.setDataSourceType(oldDataSource.getDataSourceType().charAt(0)+ newDataSource.getDataSourceType());
                           oldDataSource.setOwnerDecision("notreceived");
                           ctdb.updateDataSourceForUser(user,oldDataSource);
                           
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
            res.sendRedirect("https://"+req.getServerName()+":"+ Main.getContainerConfig().getSSLPort()+"/gsn/MyUpdateUserWaitingForDataSourceServlet");

        }
    }


}
