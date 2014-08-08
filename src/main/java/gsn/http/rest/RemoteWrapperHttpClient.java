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
* File: src/gsn/http/rest/RemoteWrapperHttpClient.java
*
* @author Ali Salehi
*
*/

package gsn.http.rest;

import gsn.beans.DataField;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class RemoteWrapperHttpClient {


	public static void main(String[] args) throws ClientProtocolException, IOException, ClassNotFoundException {
		
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		final String url = "select * from localsystemtime where timed > 10/10";

		HttpGet httpget = new HttpGet("http://localhost:22001/streaming/"+URLEncoder.encode(url, "UTF-8")+"/12345"); 
		HttpResponse response = httpclient.execute(httpget);

		ObjectInputStream out = (StreamElement4Rest.getXstream()).createObjectInputStream( (response.getEntity().getContent()));
		DataField[] structure = (DataField[]) out.readObject();
		StreamElement4Rest se = null;
		try {
			while((se = (StreamElement4Rest)out.readObject())!=null) {
				System.out.println(se.toStreamElement());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

}

