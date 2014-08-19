function FilterDeploymentName() {
    // Call the parent constructor
    AbstractFilter.call(this);

    this.deplCheckbox = [];
    this.deployments = [];
    this.deplCnt = [];
    this.deplMarkers = [];
    this.deplotmentsCnt = 0;
    this.myNumOfFilter = 0;
}

// inherit AbstractFilter
FilterDeploymentName.prototype = new AbstractFilter();

// correct the constructor pointer because it points to Person
FilterDeploymentName.prototype.constructor = FilterDeploymentName;

FilterDeploymentName.prototype.addCheckboxForDeplName = function(i, checked, markers, map){
    if (checked == true) $(".deployment").append('<input type="checkbox" id="fltr_depl_checkbox_' + i + '" checked="true"/> ' + this.deployments[i] + ' (' + this.deplCnt[this.deployments[i]] + ') ' + ' <br />');
    else  $(".deployment").append('<input type="checkbox" id="fltr_depl_checkbox_' + i + '"/> ' + this.deployments[i] + ' (' + this.deplCnt[this.deployments[i]] + ') ' + ' <br />');

    var deplCheckbox = this.deplCheckbox;
    var deployments = this.deployments;
    var deplCnt = this.deplCnt;
    var deplMarkers = this.deplMarkers;
    var myNumOfFilter = this.myNumOfFilter;

    $("#fltr_depl_checkbox_" + i).change(function () {
        var id = $(this).attr('id');
        id = id.substring(19);//at ind 19 is number in string fltr_depl_checkbox_
        i = parseInt(id);
        deplCheckbox[i] = false;
        if ($('#fltr_depl_checkbox_' + i).prop('checked'))  {
            deplCheckbox[i] = true;
        }
        for (var j = 0; j < deplCnt[deployments[i]]; j++) {
            markersCond[deplMarkers[deployments[i]][j]][myNumOfFilter] = deplCheckbox[i];
            stateChanged(deplMarkers[deployments[i]][j]);
        }
    });
}

FilterDeploymentName.prototype.addData = function(loc, ind){
    if (typeof loc['Deployment'] == 'undefined') {
        loc['Deployment'] = 'undefined';
    }
    var deplName = loc["Deployment"];
    if (typeof this.deplCnt[deplName] == 'undefined') {
        this.deplCnt[deplName] = 1;
        this.deployments[this.deplotmentsCnt++] = deplName;
        this.deplMarkers[deplName] = [];
    }
    else {
        this.deplCnt[deplName]++;
    }
    this.deplMarkers[deplName][this.deplCnt[deplName] - 1] = ind;
}

FilterDeploymentName.prototype.initialize = function(map, markers, data){

    this.deployments.sort();
    var deplCheckbox = this.deplCheckbox;
    var deployments = this.deployments;
    var deplCnt = this.deplCnt;
    var deplMarkers = this.deplMarkers;
    var myNumOfFilter = numOfFilters;
    this.myNumOfFilter = numOfFilters++;


    //Filter - Search And Filter Deployment Names
    $( "#search_depl" ).keyup(function() {
        $(".deployment").html("");
        for (var i = 0; i < deployments.length; i++) {
            if ($("#search_depl").val() != "" && deployments[i].toLowerCase().indexOf($("#search_depl").val().toLowerCase()) < 0) continue;

            var checked = deplCheckbox[i];

            if (checked == true) $(".deployment").append('<input type="checkbox" id="fltr_depl_checkbox_' + i + '" checked="true"/> ' + deployments[i] + ' (' + deplCnt[deployments[i]] + ') ' + ' <br />');
            else  $(".deployment").append('<input type="checkbox" id="fltr_depl_checkbox_' + i + '"/> ' + deployments[i] + ' (' + deplCnt[deployments[i]] + ') ' + ' <br />');

            $("#fltr_depl_checkbox_" + i).change(function () {
                var id = $(this).attr('id');
                id = id.substring(19);//at ind 19 is number in string fltr_depl_checkbox_
                i = parseInt(id);
                deplCheckbox[i] = false;
                if ($('#fltr_depl_checkbox_' + i).prop('checked'))  {
                    deplCheckbox[i] = true;
                }
                for (var j = 0; j < deplCnt[deployments[i]]; j++) {
                    markersCond[deplMarkers[deployments[i]][j]][myNumOfFilter] = deplCheckbox[i];
                    stateChanged(deplMarkers[deployments[i]][j]);
                }
            });
        }
    });


    //Deployment Names (checkboxes)
    $(".deployment").html("");
    for (var i = 0; i < this.deployments.length; i++) {
        this.addCheckboxForDeplName(i, true, markers, map);
        this.deplCheckbox[i] = true;
        deplCheckbox[i] = true;
    }
    $('#fltr_depl_clear_all').click(function () {
        for (var i = 0; i < Object.keys(data[category]).length; i++) {
            if (typeof data[category][i]['Coordinates'] == 'undefined') continue;

            markersCond[i][myNumOfFilter] = false;
            stateChanged(i);
        }
        for (var i = 0; i < deployments.length; i++) {
            $('#fltr_depl_checkbox_' + i).prop("checked", false);
            deplCheckbox[i] = false;
        }
    });
    $('#fltr_depl_check_all').click(function () {
        for (var i = 0; i < Object.keys(data[category]).length; i++) {
            if (typeof data[category][i]['Coordinates'] == 'undefined') continue;

            markersCond[i][myNumOfFilter] = true;
            stateChanged(i);
        }
        for (var i = 0; i < deployments.length; i++) {
            $('#fltr_depl_checkbox_' + i).prop("checked", true);
            deplCheckbox[i] = true;
        }
    });


}







