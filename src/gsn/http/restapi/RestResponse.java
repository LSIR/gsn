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
*
*/

package gsn.http.restapi;

import org.json.simple.JSONObject;

public class RestResponse {

    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;

    public static final String JSON_CONTENT_TYPE = "application/json";

    String Response;

    public String getResponse() {
        return Response;
    }

    public void setResponse(String response) {
        Response = response;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    String Type;     // json, xml, csv, image
    int httpStatus;

    public static RestResponse CreateErrorResponse(int httpStatus, String errorMessage) {
        RestResponse restResponse = new RestResponse();
        JSONObject jsonObject = new JSONObject();
        restResponse.setHttpStatus(httpStatus);
        restResponse.setType(JSON_CONTENT_TYPE);
        jsonObject.put("error", errorMessage);
        restResponse.setResponse(jsonObject.toJSONString());
        return restResponse;
    }

    @Override
    public String toString() {
        return "RestResponse{\n" +
                "Response='" + Response + '\'' +
                ",\n Type='" + Type + '\'' +
                ",\n httpStatus=" + httpStatus +
                "\n}";
    }
}
