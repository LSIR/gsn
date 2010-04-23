package gsn.webservice.standard;

import gsn.beans.DataField;
import gsn.http.datarequest.AggregationCriterion;
import gsn.http.datarequest.LimitCriterion;
import gsn.http.datarequest.StandardCriterion;

public interface GSNDataAccess {



    // Data Access

    //miss the vsname->fieldname[] parameter

    /**
     * This method can download data from multiple virtual sensors.
     * @param from
     * @param to
     * @param limit
     * @param condition
     * @param aggregation
     * @return
     */
    public QueryResult getMultiData(long from, long to, LimitCriterion limit, StandardCriterion[] condition, AggregationCriterion aggregation);

    public class QueryResult {
        private String vsname;
        private String executedQuery;
        //private String sid;
        //private boolean hasNext;
        private DataField header;
        private String[] tuple;

    }


    // createVirtualSensor(s)(String:content?)
    // deleteVirtualSensor(s)(String:vsname)
    // registerQuery(ies)(...)
    // unregisterQuery(ies)(queryName)
    // getListOfWrapper()
    // getListOfVirtualSensors()
    // getWrapperOutputStructure(s)(String:wrapperUrl?)                 ex: memoryusage/data/mem/memory-usage
    // getVirtualSensorOutputStructure(s)(String:virtualSensorUrl?)     ex: wan1/temp1:temp2

    // getAddressing
    //

    // getLatestData(list-of-vsnames)

    // getLatestDataAndSpecificField(vsname/field1:field2)

    // getVirtualSensor(s)Info(vsnames)

    // getData([vsname, from, to]) | what happen if the to is null, or the from is null?
    // getDataAsAttachement([vsname, from, to]), how to specifiy the date > should include the timezone.

    // getData(vsname, field, from, to)



    // how about: standard://<vsname>[<outputfield>:]
    //            standard://<vsname>/<stream-name>/<source-alias>/<wrapper-name>/[outputfield]




    // getContainerDetails

    // loggin //TODO



    // Criteria
    //limit:<offset>:<size>
    //

}
