/**
 * simulation
 */




/*
 var from = document.getElementById("from");



 function makeRequest() {
 var url = addURLParm(baseURL, "REQUEST",200);
 url = addURLParm(baseURL, "from",from);
 url = addURLParm(baseURL, "to",to);
 var request = createXMLHTTP();
 request.open("get",url,false);
 request.send(null);
 }

 */

var DataClean = {

    debugmode : false,
    theData : null,
    plot : null,
    previewPlot : null,
    orderOfDirtyPoint : null, // array for order of dirty point
    errorOfDirtyPoint : null, // array for order of dirty point

    n_sensors : 0,

    station_names2 :
            ["lafouly_st_1033","lafouly_st_1034",
                "lafouly_st_1035","lafouly_st_1036",
                "lafouly_st_1037","lafouly_st_1039",
                "lafouly_st_1040","lafouly_st_1041",
                "lafouly_st_1042","lafouly_st_1043",
                "lafouly_st_1044"],

    station_names : null,
    station_longs : null,
    station_lats : null,


    station_longs2 : [7.106459, 7.116046,
        7.12273, 7.119712,
        7.129109, 7.117759,
        7.107333, 7.108824,
        7.104324, 7.108735,
        7.115372],

    station_lats2 : [45.89898, 45.906143,
        45.904561, 45.90511,
        45.904381, 45.866703,
        45.898031, 45.896434,
        45.899146, 45.900581,
        45.865347],

    showText: function (txt) {
        alert(txt);
    },

    addURLParam : function (url, param, value) {
        url += (url.indexOf("?")) == -1 ? "?" : "&";
        url += encodeURIComponent(param) + "=" + encodeURIComponent(value);
        return url;
    },


    selectUnselect: function(stationName) {
        var list_sensors = document.getElementById("vs");

        for (var i = 0; i < list_sensors.length; i++) {
            if (list_sensors.options[i].value == stationName) {
                list_sensors.selectedIndex = i;
            }
        }
    },

    populateSensorsOptions: function() {
        //alert("populateSensorsOptions " + DataClean.n_sensors);
        var options = document.getElementById("vs");
        options.options.length = 0;
        options.options.length = DataClean.n_sensors;
        var i = 0;
        for (i = 0; i < DataClean.n_sensors; i++) {
            options.options[i].text = DataClean.station_names[i];
        }



        /*
        arrTexts = new Array();

        for (i = 0; i < lb.length; i++) {
            arrTexts[i] = lb.options[i].text;
        }

        arrTexts.sort();

        for (i = 0; i < lb.length; i++) {
            lb.options[i].text = arrTexts[i];
            lb.options[i].value = arrTexts[i];
        }
        */


    }

};
