function customRange(input) {
    return {
        minDate: (input.id == "datepicker_to" ? $("#datepicker_from").datepicker("getDate") : null),
        maxDate: (input.id == "datepicker_from" ? $("#datepicker_to").datepicker("getDate") : null)
        };
}

$(document).ready(function(){
    $('#datepicker_from,#datepicker_to').datepicker({
        showTime: true,
        timePos: 'top',
        dateFormat: 'yy-mm-dd HH:II:SS',
        beforeShow: customRange,
        showOn: "both",
        buttonImage: "/images/datepicker/calendar.png",
        buttonImageOnly: true
    });
});
