function FilterAltitude(linkID, filterDivID) {
    AbstractFilter.call(this, linkID, filterDivID);

    this.minAltitude = 100000.0;
    this.maxAltitude = -100000.0;

    this.myNumOfFilter = 1;
}

FilterAltitude.prototype = new AbstractFilter();

FilterAltitude.prototype.constructor = FilterAltitude;

FilterAltitude.prototype.updateMaxAndMinAlt = function(curAlt){
    if (curAlt > this.maxAltitude) this.maxAltitude = curAlt;
    if (curAlt < this.minAltitude) this.minAltitude = curAlt;
}

FilterAltitude.prototype.addData = function(loc, ind){
    if (typeof loc['Altitude'] != 'undefined') {
        var alt = 0;
        var altString = loc['Altitude'].trim();
        if (isNaN(altString)){
            var temp = "";
            for (var x = 0; x < altString.length; x++){
                if (!isNaN(altString.charAt(x))){
                    temp += altString.charAt(x);
                } else {
                    if (temp != ""){
                        alt = parseFloat(temp);
                        this.updateMaxAndMinAlt(alt);
                    }
                    break;
                }
            }
        } else {
            alt = parseFloat(altString);
            this.updateMaxAndMinAlt(alt);
        }
    }

}

FilterAltitude.prototype.initialize = function(map, markers, data){

    this.myNumOfFilter = numOfFilters++;
    var myNumOfFilter = this.myNumOfFilter;

    var maxAlt = this.maxAltitude;
    var minAlt = this.minAltitude;


    $("#minAlt").val(this.minAltitude);
    $("#maxAlt").val(this.maxAltitude);

    $( "#minAlt" ).keyup(function() {
        if (isNaN($("#minAlt").val())) {
            $("#minAlt").val(minAlt);
            return;
        }
        var newMax = parseFloat($("#maxAlt").val());
        var newMin = parseFloat($("#minAlt").val());

        if (newMin <= maxAlt && newMin >= minAlt){
            $("#slider-range").slider('values',0,newMin);
            if (newMin == minAlt && newMax == maxAlt){
                //reset - show all
                for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                    if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                    markersCond[i][myNumOfFilter] = true;
                    stateChanged(i);
                }
                return;
            }

            //update markers
            for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                if (typeof data[category_loc][i]['Altitude'] == 'undefined' || isNaN(data[category_loc][i]['Altitude'])) {
                    markersCond[i][myNumOfFilter] = false;
                    stateChanged(i);
                    continue;
                }
                if ((parseFloat(data[category_loc][i]['Altitude']) >= $("#minAlt").val())
                    && (parseFloat(data[category_loc][i]['Altitude']) <= $("#maxAlt").val())) {
                    markersCond[i][myNumOfFilter] = true;
                } else {
                    markersCond[i][myNumOfFilter] = false;
                }
                stateChanged(i);

            }

        } //else  $("#minAlt").val(minAlt);
    });

    $( "#maxAlt" ).keyup(function() {
        if (isNaN($("#maxAlt").val())) {
            $("#maxAlt").val(maxAlt);
            return;
        }
        var newMax = parseInt($("#maxAlt").val());
        var newMin = parseInt($("#minAlt").val());
        if (newMax <= maxAlt && newMax >= minAlt){
            $("#slider-range").slider("values", 1, newMax);
            if (newMin == minAlt && newMax == maxAlt){
                //reset - show all
                for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                    if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                    markersCond[i][myNumOfFilter] = true;
                    stateChanged(i);
                }
                return;
            }

            //update markers
            for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                if (typeof data[category_loc][i]['Altitude'] == 'undefined' || isNaN(data[category_loc][i]['Altitude'])) {
                    markersCond[i][myNumOfFilter] = false;
                    stateChanged(i);
                    continue;
                }
                if ((parseFloat(data[category_loc][i]['Altitude']) >= $("#minAlt").val())
                    && (parseFloat(data[category_loc][i]['Altitude']) <= $("#maxAlt").val())) {
                    markersCond[i][myNumOfFilter] = true;
                } else {
                    markersCond[i][myNumOfFilter] = false;
                }
                stateChanged(i);

            }

        }// else  $("#maxAlt").val(maxAlt);
    });

    $("#slider-range").slider({
        range: true,
        min: $("#minAlt").val(),
        max: $("#maxAlt").val(),
        values: [$("#minAlt").val(), $("#maxAlt").val()],
        stop: function (event, ui) {
            $("#minAlt").val(ui.values[ 0 ]);
            $("#maxAlt").val(ui.values[ 1 ]);

            if (ui.values[ 0 ] == minAlt && ui.values[ 1 ] == maxAlt){
                //reset - show all
                for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                    if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                    markersCond[i][myNumOfFilter] = true;
                    stateChanged(i);
                }
                return;
            }

            //update markers
            for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                if (typeof data[category_loc][i]['Altitude'] == 'undefined' || isNaN(data[category_loc][i]['Altitude'])) {
                    markersCond[i][myNumOfFilter] = false;
                    stateChanged(i);
                    continue;
                }
                if ((parseFloat(data[category_loc][i]['Altitude']) >= parseFloat($("#minAlt").val()))
                    && (parseFloat(data[category_loc][i]['Altitude']) <= parseFloat($("#maxAlt").val()))) {
                    markersCond[i][myNumOfFilter] = true;
                } else {
                    markersCond[i][myNumOfFilter] = false;
                }
                stateChanged(i);

            }
        }
    });



}







