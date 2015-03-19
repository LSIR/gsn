package gsn.http.rest;

import gsn.DataDistributer;
import gsn.Main;
import gsn.Mappings;
import gsn.beans.VSensorConfig;
import gsn.http.ac.DataSource;
import gsn.http.ac.GeneralServicesAPI;
import gsn.http.ac.User;
import gsn.storage.SQLUtils;
import gsn.storage.SQLValidator;
import gsn.utils.Helpers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;

public class RestStreamHanlder extends HttpServlet implements ContinuationListener {

	public static final int SUCCESS_200 = 200;

	private static final int _300 = 300;

	private static final String STREAMING = "/streaming/";

	private static transient Logger       logger     = Logger.getLogger ( RestStreamHanlder.class );
	
	protected DefaultDistributionRequest streamingReq;
	private Timer timeoutTimer = null;


	class TimeoutTimerTask extends TimerTask
	{
		public void run()
		{
			DataDistributer.getInstance(streamingReq.getDeliverySystem().getClass()).removeListener(streamingReq);
		}
	}

	public void doGet ( HttpServletRequest request , HttpServletResponse response ) throws ServletException{

		Continuation continuation = ContinuationSupport.getContinuation(request);

        if(continuation.isExpired()){
            logger.debug("Continuation has expired.");
            return;
        }

		if(continuation.isInitial()) {
            continuation.setAttribute("status", new LinkedBlockingQueue<Boolean>(1));
            continuation.addContinuationListener(this);
            continuation.suspend();
            try {
                URLParser parser = new URLParser(request);
                VSensorConfig vsConfig = parser.getVSensorConfig();
                
                //
                if ( Main.getContainerConfig().getAcConfig().isEnabled() && DataSource.isVSManaged(vsConfig.getName()) ){
                	String[] cred = parseAuthorizationHeader(request);
                    if (cred == null) {
                        try {
                            response.setHeader("WWW-Authenticate", "Basic realm=\"GSN Access Control\"");// set the supported challenge
                            response.sendError(HttpStatus.SC_UNAUTHORIZED, "Unauthorized access.");
                        }
                        catch (IOException e) {
                            logger.debug(e.getMessage(), e);
                        }
                        return;
                    }
                	
                    User user = GeneralServicesAPI.getInstance().doLogin(cred[0], cred[1]);
                    if ((user == null || (! user.isAdmin() && ! user.hasReadAccessRight(vsConfig.getName())))) {
                        response.setHeader("WWW-Authenticate", "Basic realm=\"GSN Access Control\"");// set the supported challenge
                        response.sendError(HttpStatus.SC_UNAUTHORIZED, "Unauthorized access.");
                        return;
                    }
                }
                if (parser.getTimeout() != null) {
                	timeoutTimer = new Timer("timeoutTimer");
                	timeoutTimer.schedule(new TimeoutTimerTask(), parser.getTimeout()*1000);
                }

            	long startTime = parser.getStartTime();
                if (parser.isContinuous()) {
    				Connection conn = null;
    				ResultSet rs = null;
    				try {
    					conn = Main.getStorage(vsConfig).getConnection();
    	
    					// check if table already exists
    					rs = conn.getMetaData().getTables(null, null, vsConfig.getName(), new String[] {"TABLE"});
    					if (rs.next()) {
    						StringBuilder query = new StringBuilder();
    						query.append("select max(timed) from ").append(vsConfig.getName());
    						Main.getStorage(vsConfig).close(rs);
    						rs = Main.getStorage(vsConfig).executeQueryWithResultSet(query, conn);
    						if (rs.next()) {
    							long t = rs.getLong(1);
    							if (startTime > t) {
    								startTime = t;
    								logger.info("newest local timed is older than requested start time -> using timed as start time");
    							}
    						}
    					}
    					else
    						logger.info("Table '" + vsConfig.getName() + "' doesn't exist => can not check for newest local timed");
    				} catch (SQLException e) {
    					logger.error(e.getMessage(), e);
    				} finally {
    					Main.getStorage(vsConfig).close(rs);
    					Main.getStorage(vsConfig).close(conn);
    				}
                }
                
                String remoteHost = request.getHeader("x-forwarded-for");
                if (remoteHost == null) {
                    remoteHost = request.getHeader("X_FORWARDED_FOR");
                    if (remoteHost == null) {
                        remoteHost = request.getRemoteHost();
                    }
                }
                RestDelivery deliverySystem = new RestDelivery(continuation, remoteHost, parser.getLimit());
                streamingReq = DefaultDistributionRequest.create(deliverySystem, parser.getVSensorConfig(), parser.getQuery(), startTime, parser.isContinuous());
                DataDistributer.getInstance(deliverySystem.getClass()).addListener(streamingReq);
			}catch (Exception e) {
				logger.warn(e.getMessage());
                continuation.complete();
            }
		}else {
            boolean status = false;
            try{
                status = !continuation.getServletResponse().getWriter().checkError();
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            }
            continuation.suspend();
            try {
            	logger.debug("continuation supended, set status.");
                ((LinkedBlockingQueue<Boolean>)continuation.getAttribute("status")).put(status);
            } catch (InterruptedException e) {
                logger.debug(e.getMessage(), e);
            }
        	if (((RestDelivery)streamingReq.getDeliverySystem()).isLimitReached()) {
        		if (timeoutTimer != null)
        			timeoutTimer.cancel();
        		DataDistributer.getInstance(streamingReq.getDeliverySystem().getClass()).removeListener(streamingReq);
        	}
		}
	}
	/**
	 * This happens at the server
	 */
	public void doPost ( HttpServletRequest request , HttpServletResponse response ) throws ServletException  {
        try {
			URLParser parser = new URLParser(request);
            String vsName = parser.getVSensorConfig().getName();

            //
            if (Main.getContainerConfig().getAcConfig().isEnabled() && DataSource.isVSManaged(vsName)) {
                String[] cred = parseAuthorizationHeader(request);
                if (cred == null) {
                    try {
                        response.setHeader("WWW-Authenticate", "Basic realm=\"GSN Access Control\"");// set the supported challenge
                        response.sendError(HttpStatus.SC_UNAUTHORIZED, "Unauthorized access.");
                    }
                    catch (IOException e) {
                        logger.debug(e.getMessage(), e);
                    }
                    return;
                }

                User user = GeneralServicesAPI.getInstance().doLogin(cred[0], cred[1]);
                if ((user == null || (! user.isAdmin() && ! user.hasReadAccessRight(vsName)))) {
                    response.setHeader("WWW-Authenticate", "Basic realm=\"GSN Access Control\"");// set the supported challenge
                    response.sendError(HttpStatus.SC_UNAUTHORIZED, "Unauthorized access.");
                    return;
                }
            }
            //
            Double notificationId = Double.parseDouble(request.getParameter(PushDelivery.NOTIFICATION_ID_KEY));
			String localContactPoint = request.getParameter(PushDelivery.LOCAL_CONTACT_POINT);
			if (localContactPoint == null) {
				logger.warn("Push streaming request received without "+PushDelivery.LOCAL_CONTACT_POINT+" parameter !");
				return;
			}
			//checking to see if there is an already registered notification id, in that case, we ignore (re)registeration.
			PushDelivery delivery = new PushDelivery(localContactPoint,notificationId,response.getWriter());

			boolean isExist = DataDistributer.getInstance(delivery.getClass()).contains(delivery);
			if (isExist) {
				logger.debug("Keep alive request received for the notification-id:"+notificationId);
				response.setStatus(SUCCESS_200);
				delivery.close();
				return;
			}

			DefaultDistributionRequest distributionReq = DefaultDistributionRequest.create(delivery, parser.getVSensorConfig(), parser.getQuery(), parser.getStartTime(), parser.isContinuous());
			logger.debug("Rest request received: "+distributionReq.toString());
			DataDistributer.getInstance(delivery.getClass()).addListener(distributionReq);
			logger.debug("Streaming request received and registered:"+distributionReq.toString());
		}catch (Exception e) {
			logger.warn(e.getMessage(),e);
			return ;
		}
	}
	/**
	 * This happens at the client
	 */
	public void doPut( HttpServletRequest request , HttpServletResponse response ) throws ServletException  {
		double notificationId = Double.parseDouble(request.getParameter(PushDelivery.NOTIFICATION_ID_KEY));
		PushRemoteWrapper notification = NotificationRegistry.getInstance().getNotification(notificationId);
		try {
			if (notification!=null) {
				boolean status = notification.manualDataInsertion(request.getParameter(PushDelivery.DATA));
                if (status)
                    response.setStatus(SUCCESS_200);
                else
                    response.setStatus(_300);
			}else {
				logger.warn("Received a Http put request for an INVALID notificationId: " + notificationId);
				response.sendError(_300);
			}
		} catch (IOException e) {
			logger.warn("Failed in writing the status code into the connection.\n"+e.getMessage(),e);
		}
	}

    /**
     *
     * @param request
     * @return [username,password] or null if unable to retrieve these pieces of information.
     */
    private String[] parseAuthorizationHeader(HttpServletRequest request) {
        // Get username/password from the Authorization header
        String authHeader = request.getHeader("Authorization"); // form: BASIC d2VibWFzdGVyOnRyeTJndWVTUw
        if (authHeader != null) {
            String[] ahs = authHeader.split(" ");
            if (ahs.length == 2) {
                String b64UsernamPassword = ahs[1]; // we get: d2VibWFzdGVyOnRyeTJndWVTUw
                sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
                try {
                    String userPass = new String(decoder.decodeBuffer(b64UsernamPassword)); // form: username:passsword
                    String[] ups;
                    if ((ups = userPass.split(":")).length == 2) {
                        return new String[]{
                                ups[0], // username
                                ups[1]  // password
                        };
                    }
                }
                catch (IOException e) {
                    logger.debug(e.getMessage(), e);
                }
            }
        }
        return null;
    }

	class URLParser{
		private String query,tableName;
		private long startTime;
		private Integer limit = null;
		private Integer timeout = null;
		private boolean continuous = false;
		private VSensorConfig config;
		public URLParser(HttpServletRequest request) throws UnsupportedEncodingException, Exception {
			String requestURI = request.getRequestURI().substring(request.getRequestURI().toLowerCase().indexOf(STREAMING)+STREAMING.length());
			StringTokenizer tokens = new StringTokenizer(requestURI,"/",true);
			startTime = System.currentTimeMillis();
			query = tokens.nextToken();
			query = URLDecoder.decode(query,"UTF-8");
			int pos = 0;
			while (tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				if (!token.equals("/")) {
					checkToken(URLDecoder.decode(token,"UTF-8").toLowerCase().trim());
					pos++;
				}
				if (pos > 4)
					throw new Exception("URL mall formated >" + requestURI + "<");
			}
			tableName = SQLValidator.getInstance().validateQuery(query);
			if (tableName==null)
				throw new RuntimeException("Bad Table name in the query:"+query);
			/** IMPORTANT: We change the table names to lower-case as some databases (e.g., MySQL on linux) are case sensitive and in 
			 * general we use lower case for table names in GSN. **/
			tableName=tableName.trim();
			query = SQLUtils.newRewrite(query, tableName, tableName.toLowerCase()).toString();
			tableName=tableName.toLowerCase();
			config = Mappings.getVSensorConfig(tableName);
		}
		private void checkToken(String s) throws Exception {
			if (s.startsWith("t"))
				timeout = Integer.parseInt(s.substring(1));
			else if (s.startsWith("l"))
				limit = Integer.parseInt(s.substring(1));
			else if (s.equals("c"))
				continuous = true;
			else {
				try {
					startTime = Long.parseLong(s);
				} catch (NumberFormatException e) {
					startTime= Helpers.convertTimeFromIsoToLong(s);
				}
			}
		}
		protected VSensorConfig getVSensorConfig() {
			return config;
		}
		protected String getQuery() {
			return query;
		}
		protected long getStartTime() {
			return startTime;
		}
		protected Integer getLimit() {
			return limit;
		}
		protected Integer getTimeout() {
			return timeout;
		}
		protected boolean isContinuous() {
			return continuous;
		}

	}

	@Override
	public void onComplete(Continuation continuation) {
		logger.warn("continuation completed: "+continuation);
		((LinkedBlockingQueue<Boolean>)continuation.getAttribute("status")).offer(new Boolean(false));
	}
	
	@Override
	public void onTimeout(Continuation continuation) {
		logger.warn("continuation expired: "+continuation);
		continuation.complete();
	}

}


