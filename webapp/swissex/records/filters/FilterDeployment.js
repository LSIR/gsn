function FilterDeployment(linkID, filterDivID) {
    AbstractFilter.call(this, linkID, filterDivID);

    this.listTrue = [];
    this.listTrueCnt = 0;
    this.listFalse = [];
    this.listFalseCnt = 0;

    this.myNumOfFilter = 6;
}

FilterDeployment.prototype = new AbstractFilter();

FilterDeployment.prototype.constructor = FilterDeployment;


FilterDeployment.prototype.addData = function(rec, ind){
    if (typeof rec['ServerName'] == 'undefined'){
        this.listFalse[this.listFalseCnt++] = ind;
    } else {
        this.listTrue[this.listTrueCnt++] = ind;
    }
}

FilterDeployment.prototype.initialize = function(map, markers, data, locationToInd, locationToIndCnt){
    this.myNumOfFilter = numOfFilters++;
    var myNumOfFilter = this.myNumOfFilter;

    for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
        if (typeof data[category_loc][i]['Coordinates'] == 'undefined' || locationToInd[data[category_loc][i]['Location'].trim().toLowerCase()][locationToIndCnt[data[category_loc][i]['Location'].trim().toLowerCase()]-1]['hasrecord'] == true) continue;
        var arr = [];
        arr[0] = i;
        this.listFalse[this.listFalseCnt++] = arr;
    }

    var listTrue = this.listTrue;
    var listFalse = this.listFalse;


    $("#deploy_select").change(function(){
       var val = $("#deploy_select").val();

        if (val == "all"){
            for (var i = 0; i < listTrue.length; i++){
                for (var j = 0; j < listTrue[i].length; j++){
                    markersCond[listTrue[i][j]][myNumOfFilter] = true;
                    stateChanged(listTrue[i][j]);
                }
            }
            for (var i = 0; i < listFalse.length; i++){
                for (var j = 0; j < listFalse[i].length; j++){
                    markersCond[listFalse[i][j]][myNumOfFilter] = true;
                    stateChanged(listFalse[i][j]);
                }
            }
        } else if (val == "true"){
            for (var i = 0; i < listTrue.length; i++){
                for (var j = 0; j < listTrue[i].length; j++){
                    markersCond[listTrue[i][j]][myNumOfFilter] = true;
                    stateChanged(listTrue[i][j]);
                }
            }
            for (var i = 0; i < listFalse.length; i++){
                for (var j = 0; j < listFalse[i].length; j++){
                    markersCond[listFalse[i][j]][myNumOfFilter] = false;
                    stateChanged(listFalse[i][j]);
                }
            }
        } else if (val == "false"){
            for (var i = 0; i < listTrue.length; i++){
                for (var j = 0; j < listTrue[i].length; j++){
                    markersCond[listTrue[i][j]][myNumOfFilter] = false;
                    stateChanged(listTrue[i][j]);
                }
            }
            for (var i = 0; i < listFalse.length; i++){
                for (var j = 0; j < listFalse[i].length; j++){
                    markersCond[listFalse[i][j]][myNumOfFilter] = true;
                    stateChanged(listFalse[i][j]);
                }
            }
        }

    });



}







