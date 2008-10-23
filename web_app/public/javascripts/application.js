/*************************************************/
/********METHODS USED FOR DEBUGGING **************/
/*************************************************/
function array_to_str(array){
    if (!isArray(array)){
      return array;
    }
    to_return = "[";
    var first = true;
    for (var key in array){
      if (first==false){
        to_return+= " , ";
      }
      to_return+=key+" => "+ array_to_str(array[key]);
      first = false;
    }
    to_return+=']';
    return to_return;
  }
  function isArray(obj) {
    if (obj.constructor.toString().indexOf("Array") == -1)
      return false;
    else
      return true;
  }


/*************************************************/
/********METHODS USED FOR UTILITY **************/
/*************************************************/

// EMPTY !

/***************************************/

/* DeploymentsVirtualsensorsOutputformatS */
dvos = null;

/* Mappings */
fields_to_virtual_sensor = []
fields_to_deployments = []
virtual_sensor_to_deployment = []
deployment_to_fields = []
virtual_sensor_to_fields = []

unique_counter = 0;

function option_list_key(name,items,all){
    to_return = '<select name="' + name + '" class="' + name+ '">'
    if (all){
        to_return+='<option value="All" >' + all + '</option>';
    }
    for (var item in items){
        to_return+='<option value="' + item+'" >'+item+'</option>';
    }
    to_return+='</select>'
    return to_return;
}

function option_list_value(name,items,all){
    to_return = '<select name="' + name + '" class="' + name+ '">'
    if (all!=null){
        to_return+='<option value="All" >' + all + '</option>';
    }
    for (var item in items){
        to_return+='<option value="' + items[item]+'" >'+items[item]+'</option>';
    }
    to_return+='</select>'
    return to_return;
}


function customRange(input) {
    return {
        minDate: (input.id == "datepicker_to" ? $("#datepicker_from").datepicker("getDate") : null),
        maxDate: (input.id == "datepicker_from" ? $("#datepicker_to").datepicker("getDate") : null)
    };
}

$(function(){
    $('#datepicker_from,#datepicker_to').datepicker({
        showTime: true,
        timePos: 'top',
        dateFormat: 'yy-mm-dd HH:II:SS',
        beforeShow: customRange,
        showOn: "both",
        buttonImage: "/images/datepicker/calendar.png",
        buttonImageOnly: true,
        firstDay: 1,
        yearRange: '-20:+1',
        showStatus: true,
        speed: 'fast'
    });

    $('.datepicker').datepicker({
        showTime: true,
        timePos: 'top',
        showOn: "both",
        dateFormat: 'yy-mm-dd HH:II:SS',
        buttonImage: "/images/datepicker/calendar.png",
        buttonImageOnly: true,
        firstDay: 1,
        yearRange: '-20:+1',
        showStatus: true,
        speed: 'fast'
    });

    $.getJSON("/data/dvos",
        function(data){
            dvos=data;
            for (var deployment in data){
                for (var virtual_sensor in data[deployment]){
                    virtual_sensor_to_deployment[virtual_sensor]=virtual_sensor_to_deployment[virtual_sensor]||[];
                    virtual_sensor_to_deployment[virtual_sensor].push(deployment);
                    for (var format in data[deployment][virtual_sensor]){
                        fields_to_virtual_sensor[data[deployment][virtual_sensor][format]] = fields_to_virtual_sensor[data[deployment][virtual_sensor][format]] || [];
                        fields_to_virtual_sensor[data[deployment][virtual_sensor][format]].push(virtual_sensor);

                        fields_to_deployments[data[deployment][virtual_sensor][format]] = fields_to_deployments[data[deployment][virtual_sensor][format]] || [];
                        fields_to_deployments[data[deployment][virtual_sensor][format]].push(deployment);

                        deployment_to_fields[deployment]=deployment_to_fields[deployment]||[];
                        deployment_to_fields[deployment].push(data[deployment][virtual_sensor][format]);

                        virtual_sensor_to_fields[virtual_sensor]=virtual_sensor_to_fields[virtual_sensor]||[];
                        virtual_sensor_to_fields[virtual_sensor].push(format);
                    //  alert(data[deployment][virtual_sensor][format]);
                    //alert(deployment+"---"+virtual_sensor+" --->"+data[deployment][virtual_sensor][format])
                    }
                }
            }
        // Other dependent call backs sit here.
        //alert(option_list_key('abc',fields_to_deployments));
        })
});