package tinygsn.model.utils;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class Oauth2Connection {

    private String url;
    private String clientId;
    private String clientSecret;
    private String token;

    public Oauth2Connection(String url, String clientId, String clientSecret) {
        this.url = url;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean authenticate() throws IOException {

        HttpURLConnection httpClient = (HttpURLConnection) new URL(url+"/oauth2/token").openConnection();
        httpClient.setRequestMethod("POST");
        String data = "grant_type=client_credentials";
        data += "&client_id=" + URLEncoder.encode(clientId, "UTF-8");
        data += "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8");
        httpClient.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpClient.setRequestProperty("Accept", "*/*");
        httpClient.setDoOutput(true);
        OutputStream os = httpClient.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(data);
        writer.flush();
        writer.close();
        os.close();

        System.setProperty("http.keepAlive", "false");
        httpClient.connect();
        try {
            if (httpClient.getResponseCode() != 200) {
                InputStreamReader in = new InputStreamReader(httpClient.getErrorStream(),"UTF-8");
                ServerError error = new Gson().fromJson(in, ServerError.class);
                throw new IOException("Error signing-in [" + error.code + "] - " + error.error);
            }
            InputStreamReader in = new InputStreamReader(httpClient.getInputStream(),"UTF-8");
            Token oauth_token = new Gson().fromJson(in, Token.class);
            token = oauth_token.access_token;
        }finally {
            httpClient.disconnect();
        }
        return true;
    }

    public String doJsonRequest(String method, String path, String jsonData) throws IOException{

        HttpURLConnection httpClient = (HttpURLConnection) new URL(url + path).openConnection();
        httpClient.setRequestMethod(method);
        httpClient.setRequestProperty("Authorization", "Bearer " + token);
        httpClient.setRequestProperty("Content-Type", "application/json");
        httpClient.setRequestProperty("Accept", "*/*");
        httpClient.setDoOutput(true);
        OutputStream os = httpClient.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(jsonData);
        writer.flush();
        writer.close();
        os.close();

        System.setProperty("http.keepAlive", "false");
        httpClient.connect();
        try {
            if (httpClient.getResponseCode() != 200) {
                Log.e("Oauth2Connection", "doJsonRequest: " + httpClient.getResponseMessage());
                throw new IOException("Request error: " + httpClient.getResponseMessage());
            }
            BufferedInputStream bis = new BufferedInputStream(httpClient.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int result = bis.read();
            while(result != -1) {
                buf.write((byte) result);
                result = bis.read();
            }
            return buf.toString();
        }finally {
            httpClient.disconnect();
        }
    }

    private class ServerError implements Serializable {
        int code;
        String error;
    }
    private class Token implements Serializable {

        private String expires_in;
        private String token_type;
        private String access_token;

        public String getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(String expires_in) {
            this.expires_in = expires_in;
        }

        public String getToken_type() {
            return token_type;
        }

        public void setToken_type(String token_type) {
            this.token_type = token_type;
        }

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }
}
