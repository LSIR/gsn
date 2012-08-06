package gsn.http.ac;

import org.apache.log4j.Logger;

import gsn.Main;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: May 15, 2010
 * Time: 1:27:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyGroupHtmlResultSetServlet  extends HttpServlet
{
     private static transient Logger logger                             = Logger.getLogger( MyGroupHtmlResultSetServlet.class );

    /****************************************** Servlet Methods*******************************************/
    /******************************************************************************************************/

    public void doPost(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        ConnectToDB ctdb = null;

        // Get the session
        HttpSession session = req.getSession();
        ParameterSet pm = new ParameterSet(req);
        if(pm.valueForName("groupname")==null)
        {
            res.sendRedirect("/");
        }
        else
        {
            if(pm.valueForName("groupname").equals(""))
            {
                res.sendRedirect("/");
            }
            else
            {
                try
                {
                    this.setSessionPrintWriter(req,out);
                    ctdb = new ConnectToDB();
                    this.printHeader(out,pm.valueForName("groupname"));
                    this.printLayoutMastHead(out,pm.valueForName("groupname"));
                    this.printLayoutContent(out);
                    ResultSet resultset=ctdb.selectTwoColumnsUnderOneCondition(new Column("DATASOURCENAME"),new Column("DATASOURCETYPE"), "ACGROUP_ACDATASOURCE",new Column("GROUPNAME",pm.valueForName("groupname")));
                    out.println("<br>");
                    out.println(this.resultSetToString(resultset));
                    out.println("<br>");
                }
                catch(Exception e)
                {
                    out.println("<p><b>Can not display group content!</b></p>");
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
                this.printLayoutFooter(out);
            }
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException
    {
           this.doPost(req,res);
    }

    /****************************************** HTML Printing Methods*******************************************/
    /***********************************************************************************************************/


    private void printHeader(PrintWriter out, String groupName)
	{
        out.println("<HTML>");
        out.println("<HEAD>");
		out.println("<TITLE>"+ groupName +" Structure</TITLE>");
        out.println(" <link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/style/acstyle.css\"/>");
        //printStyle(out);
        out.println("</HEAD>");
        out.println("<body>");
        out.println("<div id=\"container\">");
        out.println("<div class=box>");

	}
    private void printLayoutMastHead(PrintWriter out, String groupName)
    {
        out.println("<div id=\"masthead\">");
        out.println("<h1><a id=\"gsn-name\" style=\"\" href=\"/\">" + Main.getContainerConfig( ).getWebName( ) + "</a></h1>");

        out.println("</div>");
        out.println("<div id=\"navigation\">");
        out.println("<div id=\"menu\">");
        this.printLinks(out, groupName);
        out.println("</div>");
        out.println("</div>");
    }
    private void printLayoutContent(PrintWriter out)
    {
        out.println("<div id=\"content\">");
    }

    private void printLinks(PrintWriter out, String groupName)
    {
        out.println("<li class=\"selected\" style=\"font-size:0.8em;font-weight:bolder;padding: 0 8px;text-decoration:none;text-transform:uppercase;\">"+ groupName +" structure</li>");
        out.println("<li><a href=\"JavaScript:window.close()\">Close</a></li>");

    }

    private void printLayoutFooter(PrintWriter out)
    {
        out.println("</div>");//content
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
        out.println("<BR>");
        //out.println("<HR>");
    }


    /****************************************** AC related Methods*****************************************************/
    /******************************************************************************************************************/

    private String resultSetToString(ResultSet resultset)// can be called at most once
    {
        StringBuffer out = new StringBuffer();
        // Start a table to display the result set
        out.append("<table class=tab>\n");
        try
        {
            ResultSetMetaData rsmd = resultset.getMetaData();
            int numcols = rsmd.getColumnCount();

            // Title the table with the result set's column labels
            out.append("<TR>");
            for (int i = 1; i <= numcols; i++)
            {
                if(rsmd.getColumnLabel(i).equals("DATASOURCENAME"))
                {
                   out.append("<TH>" + "virtual sensor name");
                }
                else if(rsmd.getColumnLabel(i).equals("DATASOURCETYPE"))
                {
                     out.append("<TH>" + "access right");
                }
                else
                {
                    out.append("<TH>" + rsmd.getColumnLabel(i));
                }

            }
            out.append("</TR>\n");

            while(resultset.next())
            {
                out.append("<TR>"); // start a new row
                for (int i = 1; i <= numcols; i++)
                {
                    out.append("<TD>"); // start a new data element
                    Object obj = resultset.getObject(i);
                    if (obj != null)
                    {
                        if(obj.toString().equals("1"))
                        {
                            out.append("read");
                        }
                        else if(obj.toString().equals("2"))
                        {
                            out.append("write");
                        }
                        else if(obj.toString().equals("3"))
                        {
                            out.append("read/write");
                        }
                        else //!! do not call a virtual sensor by number
                        {
                            out.append(obj.toString());
                        }
                    }
                    else
                        out.append("&nbsp;");
                }
                out.append("</TR>\n");
            }

            // End the table
            out.append("</TABLE>\n");
        }
        catch (SQLException e)
        {
            out.append("</table class=tab><H1>ERROR:</H1> " + e.getMessage() + "\n");
        }
        return out.toString();
    }


    /****************************************** Client Session related Methods*******************************************/
    /********************************************************************************************************************/

    private void setSessionPrintWriter(HttpServletRequest req,PrintWriter out)
    {
        req.getSession().setAttribute("out",out);
    }












}
