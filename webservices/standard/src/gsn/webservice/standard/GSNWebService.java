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
* File: webservices/standard/src/gsn/webservice/standard/GSNWebService.java
*
* @author Timotee Maret
*
*/

package gsn.webservice.standard;

import gsn.http.datarequest.AggregationCriterion;
import gsn.http.datarequest.StandardCriterion;

public interface GSNWebService {

    // --------------
    // Data Access
    // --------------

    public QueryResult[] getMultiData(FieldSelector[] virtualSensors, long from, long to, int nb, StandardCriterion[] conditions, AggregationCriterion aggregation, String timeFormat);

    public QueryResult[] getNextData(String sid);

    public QueryResult[] getLatestMultiData(FieldSelector[] virtualSensors, String timeFormat);

    // -------------
    // Management
    // -------------

    public boolean createVirtualSensor(String vsname, String descriptionFileContent);

    public boolean deleteVirtualSensor(String vsname);

    public boolean registerQuery(String queryName, DataField[] outputStructure, String[] vsnames, String query);

    public boolean unregisterQuery(String queryName);

    //

    public VirtualSensorDetails[] getVirtualSensorsDetails(FieldSelector[] virtualSensors, String[] infos);

    public ContainerDetails getContainerInfo();

    //

    public String[] listVirtualSensorNames();

    public String[] listWrapperURLs();

    // Beans

    public class FieldSelector {
        public String vsname;
        public String[] fieldNames;
    }

    public class QueryResult {
        public String vsname;
        public String executedQuery;
        public String sid;
        public boolean hasNext;
        //public DataField header;
        public DataField[] streamElement;
    }

    public class StreamElement {
        public DataField[] fields;
    }

    public class DataField {
        public String description;
        public String name;
        public String type;
        public String value;
    }

    public class ContainerDetails {
        public String name;                 // opt
        public String author;               // opt
        public String email;                // opt
        public String description;          // opt
        public int port;                    // mandatory
        public String timeFormat;           // opt
        public int sslPort;                 // opt
        public String sslKeystorePassword;  //opt
        public String sslKeyPassword;       // opt
    }

    public class VirtualSensorDetails {
        public String vsname;
        public ConfInfo info;
        public ConfProcessor processor;
        public ConfAddressing addressing;
        public ConfOutputStructure outputStructure;
        public ConfWrapper wrapper;
        public ConfWrapperOutputFormat wrapperOutputFormat;
    }

    public class ConfInfo {
        public String author;
        public String email;
        public String description;
        public String rate;
    }

    public class ConfProcessor {
        public String className;
        public ConfPredicate[] initParams;
    }

    public class ConfAddressing {
        public ConfPredicate[] predicates;
    }

    public class ConfOutputStructure {
        public DataField[] fields;
    }

    public class ConfPredicate {
        public String name;
        public String value;
    }

    public class ConfWrapper {
        public String wrapper;
        public WrapperURL wrapperURL;
        public ConfPredicate[] predicates;
    }

    public class ConfWrapperOutputFormat {
        public DataField[] outputFormat;
    }

    public class WrapperURL {
        public String virtualSensor;
        public String stream;
        public String source;
        public String wrapper;
    }
}
