/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 *
 * This file is part of GSN.
 *
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GSN.  If not, see <http://www.gnu.org/licenses/>.
 *
 * File: src/gsn/http/restapi/RestResponse.java
 *
 * @author Sofiane Sarni
 * @author Milos Stojanovic
 *
 */

package gsn.http.restapi;

import java.util.HashMap;
import java.util.Map;

public class RestResponse {

    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;
    public static final int HTTP_STATUS_ERROR = 203;

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String CSV_CONTENT_TYPE = "text/csv";

    public static final String RESPONSE_HEADER_CONTENT_DISPOSITION_NAME = "Content-Disposition";
    public static final String RESPONSE_HEADER_CONTENT_DISPOSITION_VALUE = "attachment;filename=\"%s\"";

    private String response;
    private String type;     // json, xml, csv, image
    private int httpStatus;
    private HashMap<String, String> headers = new HashMap<String, String>();

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void addHeader(String key, String value){
        headers.put(key, value);
    }

    public String getHeaderValue(String key){
        return headers.get(key);
    }

    public String removeHeaderKey(String key){
        return headers.remove(key);
    }
    
    public Map<String, String> getHeaders(){
    	return headers;
    }

    @Override
    public String toString() {
        return "RestResponse{\n" +
                "Response='" + response + '\'' +
                ",\n Type='" + type + '\'' +
                ",\n httpStatus=" + httpStatus +
                "\n}";
    }
}