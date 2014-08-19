function FilterExperiment(linkID, filterDivID) {
    AbstractFilter.call(this, linkID, filterDivID);

    this.mediums = [];
    this.mediumsToCnt = [];
    this.mediumsNameToMarkers = [];
    this.mediumsCnt = 0;
    this.medChecked = [];
    this.myNumOfFilter = 3;
    this.initUndefined = false;

    this.name = "experiments";
    this.divclass = ".experiment";
    this.checkBoxId = "fltr_expe_checkbox_";
    this.searchId = "#search_experiment";
    this.clearAllId = "#fltr_expr_clear_all";
    this.checkAllId = "#fltr_expr_check_all";
}

FilterExperiment.prototype = new AbstractFilter();

FilterExperiment.prototype.constructor = FilterExperiment;


FilterExperiment.prototype.addData = function(loc, ind){

    var exp = loc[this.name];
    if (typeof loc[this.name] == 'undefined' || loc[this.name] == '') {
        exp = 'undefined';
    }
    if (typeof this.mediumsToCnt[exp] == 'undefined') {
        this.mediumsToCnt[exp] = 1;
        this.mediums[this.mediumsCnt++] = exp;
        this.mediumsNameToMarkers[exp] = [];
    }
    else {
        this.mediumsToCnt[exp]++;
    }
    this.mediumsNameToMarkers[exp][this.mediumsToCnt[exp] - 1] = ind;
}

FilterExperiment.prototype.initialize = function(map, markers, data){

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


