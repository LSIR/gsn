function FilterRecord(name, divclass, chbxId, searchId, clAllId, chckAllId, linkID, filterDivID) {
    AbstractFilter.call(this, linkID, filterDivID);

    this.mediums = [];
    this.mediumsToCnt = [];
    this.mediumsNameToMarkers = [];
    this.mediumsCnt = 0;
    this.medChecked = [];
    this.myNumOfFilter = 3;
    this.initUndefined = false;

    this.name = name;
    this.divclass = divclass;
    this.checkBoxId = chbxId;
    this.searchId = searchId;
    this.clearAllId = clAllId;
    this.checkAllId = chckAllId;
}

FilterRecord.prototype = new AbstractFilter();

FilterRecord.prototype.constructor = FilterRecord;


FilterRecord.prototype.addData = function(rec, ind){

    var listOfMeds = [];
    var curMedia = rec[this.name];
    if (typeof curMedia == 'undefined'  || curMedia == "") {
        listOfMeds[0] = "undefined";
    } else {
        var listOfMedsT = curMedia.split(',');
        var counter = 0;
        var same = [];
        for (var k = 0; k < listOfMedsT.length; k++) {
	    listOfMedsT[k] = listOfMedsT[k].toLowerCase().trim();
            if (listOfMedsT[k] == "" || listOfMedsT[k] == " ") {
                listOfMedsT[k] = "undefined";
            }
            if (typeof same[listOfMedsT[k]] == "undefined") {
                same[listOfMedsT[k]] = 1;
                listOfMeds[counter++] = listOfMedsT[k];
            }
        }
    }

    for (var k = 0; k < listOfMeds.length; k++) {
        if (typeof this.mediumsToCnt[listOfMeds[k]] == 'undefined') {
            this.mediumsToCnt[listOfMeds[k]] = 1;
            this.mediums[this.mediumsCnt++] = listOfMeds[k];
            this.mediumsNameToMarkers[listOfMeds[k]] = [];
        }
        else this.mediumsToCnt[listOfMeds[k]]++;
    }

    for (var k = 0; k < listOfMeds.length; k++) {
        for (var j = 0; j < ind.length; j++) {
            this.mediumsNameToMarkers[listOfMeds[k]][this.mediumsToCnt[listOfMeds[k]] - 1 + j] = ind[j];
        }
        this.mediumsToCnt[listOfMeds[k]] += ind.length - 1;
    }
}

FilterRecord.prototype.initialize = function(map, markers, data, locationToInd, locationToIndCnt){



    if (this.initUndefined == false){
        this.initUndefined = true;
        var med = "undefined";
        for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
            if (typeof data[category_loc][i]['Coordinates'] == 'undefined' || locationToInd[data[category_loc][i]['Location'].trim().toLowerCase()][locationToIndCnt[data[category_loc][i]['Location'].trim().toLowerCase()]-1]['hasrecord'] == true) continue;
            if (typeof this.mediumsToCnt[med] == 'undefined') {
                this.mediumsToCnt[med] = 1;
                this.mediums[this.mediumsCnt++] = med;
                this.mediumsNameToMarkers[med] = [];
            } else this.mediumsToCnt[med]++;

            this.mediumsNameToMarkers[med][this.mediumsToCnt[med] - 1] = i;
        }
    }
    this.mediums.sort();

    this.myNumOfFilter = numOfFilters++;
    var myNumOfFilter = this.myNumOfFilter;
    var mediums = this.mediums;
    var mediumNameToMarkers = this.mediumsNameToMarkers;
    var mediumToCnt = this.mediumsToCnt;
    var medChecked = this.medChecked;

    var divclass = this.divclass;
    var checkBoxId = this.checkBoxId;
    var searchId = this.searchId;
    var clearAllId = this.clearAllId;
    var checkAllId = this.checkAllId;

    $(divclass).html("");
    for (var i = 0; i < this.mediums.length; i++) {
        $(divclass).append('<input type="checkbox" id="' + checkBoxId + i + '" checked="true"/> ' + this.mediums[i] + ' (' + this.mediumsToCnt[this.mediums[i]] + ') ' + ' <br />');
        this.medChecked[i] = true;
        medChecked[i] = true;
        $("#" + checkBoxId + i).change(function () {
            var id = $(this).attr('id');
            id = id.substring(19);//at ind 19 is number in string fltr_depl_checkbox_
            i = parseInt(id);
            var chckd = $("#" + checkBoxId + i).prop('checked');
            medChecked[i] = chckd;
            for (var j = 0; j < mediumToCnt[mediums[i]]; j++) {
                markersCond[mediumNameToMarkers[mediums[i]][j]][myNumOfFilter] = chckd;
                stateChanged(mediumNameToMarkers[mediums[i]][j]);
            }

        });
    }

    $(searchId).keyup(function() {
        $(divclass).html("");
        for (var i = 0; i < mediums.length; i++) {
            if ($(searchId).val() != "" && mediums[i].toLowerCase().indexOf($(searchId).val().toLowerCase()) < 0) continue;

            var checked = medChecked[i];

            if (checked == true) $(divclass).append('<input type="checkbox" id="' + checkBoxId + i + '" checked="true"/> ' + mediums[i] + ' (' + mediumToCnt[mediums[i]] + ') ' + ' <br />');
            else  $(divclass).append('<input type="checkbox" id="' + checkBoxId + i + '"/> ' + mediums[i] + ' (' + mediumToCnt[mediums[i]] + ') ' + ' <br />');

            $("#" + checkBoxId + i).change(function () {
                var id = $(this).attr('id');
                id = id.substring(19);//at ind 19 is number in string fltr_depl_checkbox_
                i = parseInt(id);
                medChecked[i] = false;
                if ($("#" + checkBoxId + i).prop('checked'))  {
                    medChecked[i] = true;
                }
                for (var j = 0; j < mediumToCnt[mediums[i]]; j++) {
                    markersCond[mediumNameToMarkers[mediums[i]][j]][myNumOfFilter] = medChecked[i];
                    stateChanged(mediumNameToMarkers[mediums[i]][j]);
                }
            });
        }
    });


    $(clearAllId).click(function () {
        for (var i = 0; i < mediums.length; i++) {
            for (var j = 0; j < mediumToCnt[mediums[i]]; j++) {
                markersCond[mediumNameToMarkers[mediums[i]][j]][myNumOfFilter] = false;
                stateChanged(mediumNameToMarkers[mediums[i]][j]);
            }
            $('#' + checkBoxId + i).prop("checked", false);
            medChecked[i] = false;
        }
    });
    $(checkAllId).click(function () {
        for (var i = 0; i < mediums.length; i++) {
            for (var j = 0; j < mediumToCnt[mediums[i]]; j++) {
                markersCond[mediumNameToMarkers[mediums[i]][j]][myNumOfFilter] = true;
                stateChanged(mediumNameToMarkers[mediums[i]][j]);
            }
            $('#' + checkBoxId + i).prop("checked", true);
            medChecked[i] = true;
        }
    });



}







