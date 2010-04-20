package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.SQLException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

public class RestRemoteWrapper extends AbstractWrapper {

    private final XStream XSTREAM = StreamElement4Rest.getXstream();

    private final transient Logger logger = Logger.getLogger(RestRemoteWrapper.class);

    private DataField[] structure = null;

    private DefaultHttpClient httpclient;

    private long lastReceivedTimestamp = -1;

    private ObjectInputStream inputStream;

    private HttpResponse response;

    private HttpParams getHttpClientParams(int timeout) {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setTcpNoDelay(params, false);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setConnectionTimeout(params, 30 * 1000);    // Set the connection time to 30s
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpProtocolParams.setUserAgent(params, "GSN-HTTP-CLIENT");
        return params;
    }

    public DataField[] getOutputFormat() {
        return structure;
    }

    private RemoteWrapperParamParser params;

    public String getWrapperName() {
        return "Rest Remote Wrapper";
    }

    public boolean initialize() {
        try {
            params = new RemoteWrapperParamParser(getActiveAddressBean(), false);
            httpclient = new DefaultHttpClient(getHttpClientParams(params.getTimeout()));

            lastReceivedTimestamp = params.getStartTime();
            structure = connectToRemote();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }


    public DataField[] connectToRemote() throws IOException, ClassNotFoundException {
        HttpGet httpget = new HttpGet(params.getRemoteContactPointEncoded(lastReceivedTimestamp));
        HttpContext localContext = new BasicHttpContext();

        structure = null;
        int tries = 0;
        while (tries < 2) {
            tries++;
            try {
                if (inputStream != null) {
                    try {
                        response.getEntity().consumeContent();
                        inputStream.close();
                        inputStream = null;
                    }
                    catch (Exception e) {
                        logger.debug(e.getMessage(), e);
                    }
                }
                response = httpclient.execute(httpget, localContext);
                int sc = response.getStatusLine().getStatusCode();
                AuthState authState = null;
                if (sc == HttpStatus.SC_UNAUTHORIZED) {
                    // Target host authentication required
                    authState = (AuthState) localContext.getAttribute(ClientContext.TARGET_AUTH_STATE);
                }
                if (sc == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    // Proxy authentication required
                    authState = (AuthState) localContext.getAttribute(ClientContext.PROXY_AUTH_STATE);
                }

                if (authState != null) {
                    if (params.getUsername() == null || tries > 1) {
                        logger.error("A valid username/password required to connect to the remote host: " + params.getRemoteContactPoint());
                    } else {
                        AuthScope authScope = authState.getAuthScope();
                        Credentials creds = new UsernamePasswordCredentials(params.getUsername(), params.getPassword());
                        httpclient.getCredentialsProvider().setCredentials(authScope, creds);
                    }
                } else {
                    logger.debug(new StringBuilder().append("Wants to consume the strcture packet from ").append(params.getRemoteContactPoint()));
                    inputStream = XSTREAM.createObjectInputStream(response.getEntity().getContent());
                    structure = (DataField[]) inputStream.readObject();
                    logger.warn("Connection established to the remote host: " + params.getRemoteContactPoint());
                    break;
                }
            }
            catch (RuntimeException ex) {
                // In case of an unexpected exception you may want to abort
                // the HTTP request in order to shut down the underlying
                // connection and release it back to the connection manager.
                httpget.abort();
                throw ex;
            }
        }
        if (structure == null)
            throw new RuntimeException("Cannot connect to the remote host: " + params.getRemoteContactPoint());
        return structure;
    }

    public void dispose() {
        try {
            httpclient.getConnectionManager().shutdown(); //This closes the connection already in use by the response
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public void run() {
        StreamElement4Rest se = null;
        while (isActive()) {
            try {
                while (isActive() && (se = (StreamElement4Rest) inputStream.readObject()) != null) {
                    StreamElement streamElement = se.toStreamElement();
                    boolean status = manualDataInsertion(streamElement);
                    if (!status && inputStream != null) {
                        response.getEntity().consumeContent();
                        inputStream.close();
                        inputStream = null;
                    }
                }
            }
            catch (Exception e) {
                logger.warn("Connection to the remote host: " + params.getRemoteContactPoint() + " is lost, trying to reconnect in 3 seconds...");
                try {
                    if (isActive()) {
                        Thread.sleep(3000);
                        connectToRemote();
                    }
                } catch (Exception err) {
                    logger.debug(err.getMessage(), err);
                }
            }
        }
    }

    public boolean manualDataInsertion(StreamElement se) {
        try {
            // If the stream element is out of order, we accept the stream element and wait for the next (update the last received time and return true)
            if (isOutOfOrder(se)) {
                lastReceivedTimestamp = se.getTimeStamp();
                return true;
            }
            // Otherwise, we first try to insert the stream element.
            // If the stream element was inserted succesfully, we wait for the next,
            // otherwise, we return false.
            boolean status = postStreamElement(se);
            if (status)
                lastReceivedTimestamp = se.getTimeStamp();
            return status;
        }
        catch (SQLException e) {
            logger.warn(e.getMessage(), e);
            return false;
        }
    }
}
