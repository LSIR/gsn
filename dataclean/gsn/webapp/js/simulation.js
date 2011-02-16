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

var Sim = {

    REQUEST                   : "REQUEST",
    REQUEST_INIT_SIMULATION   : 200,
    REQUEST_STOP_SIMULATION   : 201,
    REQUEST_START_SIMULATION  : 202,
    REQUEST_SET_PARAMETERS    : 203,
    REQUEST_GET_LATEST_STATUS : 204,
    REQUEST_GET_LATEST_RESULT : 205,
    REQUEST_GET_IMAGE         : 206,
    REQUEST_GET_STATUS        : 207,
    REQUEST_FETCH_RESULTS     : 208,

    UNSUPPORTED_REQUEST_ERROR : 400,
    PARAM_STATIONS : "stations",
    PARAM_FROM : "from",
    PARAM_TO : "to",
    PARAM_INTERPOLATIONS : "interpolations",
    PARAM_FILTERS : "filters",
    PARAM_WINDOWSIZE : "window",

    SEPARATOR : ";",

    SIM_PROGRESS_DELAY : 60000, // every minute

    image : null,
    initialized : false,

    timer_is_on : 0,
    c : 0,
    t : null,

    timedCount : function() {
    //alert ("inside timedCount");
                    var arg = "simulation?REQUEST=207";
                    $.getJSON(arg,
                        function(data){
				        document.getElementById("txt").value = data.message;
				        if (data.finished) {
				            var img = document.getElementById('image');
        	                            img.src = "";
                                            img.width = 1;
                                            img.height = 1;
                                       }
                    } );
                    //document.getElementById("txt").value = Sim.c;
                    //Sim.c = Sim.c+1;
                    if (Sim.timer_is_on) // if not stopped
                        Sim.t=setTimeout("Sim.timedCount()",10000);
                 },

    doTimer : function() {
    //alert("doTimer");
                if (!Sim.timer_is_on) {
                        Sim.timer_is_on = 1;
                        Sim.timedCount();
                    }
                },
    stopTimer : function() {
        Sim.timer_is_on = 0;
    },

    station_names : ["lafouly_st_1033","lafouly_st_1034",
                    "lafouly_st_1035","lafouly_st_1036",
                    "lafouly_st_1037","lafouly_st_1039",
                    "lafouly_st_1040","lafouly_st_1041",
                    "lafouly_st_1042","lafouly_st_1043",
                    "lafouly_st_1044"],

    station_longs : [7.106459, 7.116046,
                     7.12273, 7.119712,
                     7.129109, 7.117759,
                     7.107333, 7.108824,
                     7.104324, 7.108735,
                     7.115372],
    station_lats : [45.89898, 45.906143,
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

    initSimulation : function() {
        var from = document.getElementById("from").value;
        var to = document.getElementById("to").value;  //
        var arg = "simulation?REQUEST=200&from=" + from
                  + "&to=" + to
                  +"&stations="+Sim.getListOfStation()
                  +"&filters="+Sim.getFilters()
                  +"&interpolations="+Sim.getInterpolations()
                  +"&window="+Sim.getWindowSize();
        //alert(arg);

        $.getJSON(arg,
            function(data){
				//alert(data.message);
				//alert("Simulation initialized.");
            } );
        //alert("List of stations: "+Sim.getListOfStation());
        Sim.doTimer();
    },

    startSimulation: function () {
        var arg = "simulation?REQUEST=202";
        //alert(arg);

        $.getJSON(arg,
            function(data){
				//alert(data.message);
            } );
        //alert("Now, starting simulation "+ Sim.getListOfStation());

        var img = document.getElementById('image');
        	img.src = "style/ajax-loader.gif";
        	img.width = 32;
        	img.height = 32;
    },

    stopSimulation: function () {
        alert("Now, stopping simulation "+ Sim.getListOfStation());
                    Sim.doTimer();
    },

    getStatus: function () {
        var arg = "simulation?REQUEST=207";
        $.getJSON(arg,
            function(data){
				alert(data.message);
            } );
    },

    getListOfStation: function() {
        var stations = document.getElementById("stations");
        l="";
        for (var i = 0; i < stations.length; i++) {
            if (stations.options[i].selected) {
                l = l + stations.options[i].value+",";
            }
        }

        return l;
    },

    addFilter: function () {

        var s = "";

        var field = document.getElementById("fieldFilter");
        var filterType = document.getElementById("filterType");
        var filterArg = document.getElementById("filterArg");

        s = field.value + "::filter1 = " + filterType.value+ ";" + field.value + "::arg1 = " + filterArg.value;

        var listFilters = document.getElementById("listFilters");
        var opt = document.createElement("option");
        opt.text=s;
        opt.value=s;
        listFilters.options.add(opt);
    },

    removeFilter: function () {
        var listFilters = document.getElementById("listFilters");
        var i = listFilters.selectedIndex;
        alert(i);
        if (i != -1) {
            listFilters.remove(i);
        }
    },

    getFilters: function () {
        var listFilters = document.getElementById("listFilters");
        s = "";
        for (var i = 0; i < listFilters.length; i++) {
                s = s +  listFilters.options[i].value + Sim.SEPARATOR;
        }
        return s;
    },

    addInterpolation: function () {
        var s = "";

        var fieldInterpolation = document.getElementById("fieldInterpolation");
        var interpolationsArg = document.getElementById("interpolationsArg");

        s = fieldInterpolation.value + "::algorithms = " + interpolationsArg.value;

        var listFilters = document.getElementById("listInterpolations");
        var opt = document.createElement("option");
        opt.text = s;
        opt.value = s;
        listFilters.options.add(opt);
    },

    removeInterpolation: function () {
        var listInterpolations = document.getElementById("listInterpolations");
        var i = listInterpolations.selectedIndex;
        alert(i);
        if (i != -1) {
            listInterpolations.remove(i);
        }
    },

    getInterpolations: function () {
        var listInterpolations = document.getElementById("listInterpolations");
        s = "";
        for (var i = 0; i < listInterpolations.length; i++) {
                s = s +  listInterpolations.options[i].value + Sim.SEPARATOR;
        }
        return s;
    },

    getWindowSize: function () {
        var windowSize = document.getElementById("windowSize");
        return windowSize.value;
    },

    getResults: function() {
        //Sim.fetchAllResults();
        var i=1;
        for (i=1;i<=6;i++) {
            var img = document.getElementById('layer_'+i);
        	    img.src = "simulation?REQUEST=206&layer="+i+"&cache-breaker="+ new Date().getTime();
        	    img.width = 450;
        	    img.height = 450;
        	}
    },

    fetchAllResults: function() {
       var arg = "simulation?REQUEST=208";
        $.getJSON(arg,
            function(data){
				alert(data.message);
            } )
    },

    startSpin: function() {
        var img = document.getElementById('image');
        	img.src = "style/ajax-loader.gif";
        	img.width = 32;
        	img.height = 32;
    },

    stopSpin: function() {
         var img = document.getElementById('image');
        	img.src = "";
        	img.width = 32;
        	img.height = 32;
    },

    selectUnselect: function(stationName) {
        var list_sensors = document.getElementById("stations");
        for (var i = 0; i < list_sensors.length; i++) {
            if (list_sensors.options[i].value == stationName) {
                list_sensors.options[i].selected = !list_sensors.options[i].selected ;
            }
}
    }

};
