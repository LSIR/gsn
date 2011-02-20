var DataClean = {

    debugmode : false,
    theData : null,
    plot : null,
    previewPlot : null,
    orderOfDirtyPoint : null,
    errorOfDirtyPoint : null,

    n_sensors : 0,

    station_names : null,
    station_longs : null,
    station_lats : null,
    station_fields : null,

    map_markers : null,

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
        DataClean.onSelectChange();
    },

    onSelectChange: function() {
        var comboFields = document.getElementById("fieldname");
        var comboSensors = document.getElementById("vs");

        var index = comboSensors.options.selectedIndex;
        var n_fields = DataClean.station_fields[index].length;

        comboFields.options.length = 0;   //clear combo
        comboFields.options.length = n_fields; // recreate combo

        var i;
        for (i = 0; i < n_fields; i++) {

            comboFields.options[i].text = DataClean.station_fields[index][i];
            comboFields.options[i].value = DataClean.station_fields[index][i];
        }

        DataClean.map_markers[index].openInfoWindowHtml(DataClean.station_names[index]);
    },

    initSensorsComboBox: function(index) {
        var comboFields = document.getElementById("fieldname");
        var comboSensors = document.getElementById("vs");

        var n_fields = DataClean.station_fields[index].length;

        comboFields.options.length = 0;   //clear combo
        comboFields.options.length = n_fields; // recreate combo

        var i;
        for (i = 0; i < n_fields; i++) {

            comboFields.options[i].text = DataClean.station_fields[index][i];
            comboFields.options[i].value = DataClean.station_fields[index][i];
        }
    },

    populateSensorsOptions: function() {
        var comboSensors = document.getElementById("vs");
        comboSensors.options.length = 0;
        comboSensors.options.length = DataClean.n_sensors;
        var i = 0;
        for (i = 0; i < DataClean.n_sensors; i++) {
            comboSensors.options[i].text = DataClean.station_names[i];
            comboSensors.options[i].value = DataClean.station_names[i];
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
    },

    startSpin: function() {
        var img = document.getElementById('imageholder');
        	img.src = "style/ajax-loader.gif";
        	img.width = 32;
        	img.height = 32;
    },

    stopSpin: function() {
         var img = document.getElementById('imageholder');
        	img.src = "";
        	img.width = 0;
        	img.height = 0;
    }
};

