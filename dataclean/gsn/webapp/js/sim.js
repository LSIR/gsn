/**
 * simulation
 */




var Sim = {

    REQUEST                 : "REQUEST",
    REQUEST_INIT_SIMULATION : 200,
    REQUEST_STOP_SIMULATION : 201,
    REQUEST_START_SIMULATION : 202,
    REQUEST_SET_PARAMETERS : 203,
    REQUEST_GET_LATEST_STATUS : 204,
    REQUEST_GET_LATEST_RESULT : 205,

    UNSUPPORTED_REQUEST_ERROR : 400,
    PARAM_STATIONS : "stations",
    PARAM_FROM : "from",
    PARAM_TO : "to",
    PARAM_INTERPOLATIONS : "interpolations",
    PARAM_FILTERS : "filters",

    showText: function (txt) {
        alert(txt);
    },

    initSimulation : function() {
        var from = document.getElementById("from").value;
        var to = document.getElementById("to").value;  //
        var arg = "simulation?REQUEST=200&from=" + from + "&to=" + to;
        alert(arg);     

    $.getJSON(arg,
        function(data){
				alert(data);
          } );


        alert("sim.js");
    },

    start: function () {
        alert("starting simulation "+ Sim.listOfStations+"\n"+Sim.PARAM_INTERPOLATIONS);
    },

    listOfStations : "list",

    theData : null

};