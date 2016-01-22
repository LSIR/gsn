function include(filename)
{
    var head = document.getElementsByTagName('head')[0];
   
    script = document.createElement('script');
    script.src = filename;
    script.type = 'text/javascript';
   
    head.appendChild(script)
}

//include("http://d3js.org/d3.v3.min.js");
include("js/d3plots/d3.v3.min.js");

function drawTimeseries(dataset, nb){
	drawChartTimeseries(parseDataTimeseries(dataset));
}

var selectedTimeRangeHistory = [];
var currentTimeRangeShown = 0;

var allData;

function parseDataTimeseries(dataset){
	var keys = d3.keys(dataset);
    var data = [];
    var minDate = null;
    var maxDate = null;
    for (var i = 0; i < keys.length; i++) {
        if (keys[i].indexOf("aggregation_interval") > -1) continue;
        data[i] = [];
        data[i]['name'] = keys[i];
        data[i]['values'] = [];
        var cnt = 0;
        for (var j = 0; j < dataset[keys[i]].data.length; j++) {
            if (isNaN(dataset[keys[i]].data[j][1])) continue;
            if (minDate == null){
                minDate = new Date(dataset[keys[i]].data[j][0]);
                maxDate = new Date(dataset[keys[i]].data[j][0]);
            } else {
                if (minDate.getTime() > new Date(dataset[keys[i]].data[j][0]).getTime()) minDate = new Date(dataset[keys[i]].data[j][0]);
                if (maxDate.getTime() < new Date(dataset[keys[i]].data[j][0]).getTime()) maxDate = new Date(dataset[keys[i]].data[j][0]);
            }
            data[i]['values'][cnt] = [];
            data[i]['values'][cnt]['date'] = new Date(dataset[keys[i]].data[j][0]);
            data[i]['values'][cnt]['sensor_value'] = dataset[keys[i]].data[j][1];
            cnt++;
        }
    }
    selectedTimeRangeHistory[0] = {min: minDate, max: maxDate};
    allData = data;
	return data;
}

function drawChartTimeseries(data){
	
	var color = d3.scale.category20();
	
	var margin = {top: 20, right: 10, bottom: 150, left: 90},
		width = 700 - margin.left - margin.right,
		height = 700 - margin.top - margin.bottom;
	
	var x = d3.time.scale()
		.range([0, width])

	var y = d3.scale.linear()
		.range([height, 0]);

	var xAxis = d3.svg.axis()
		.scale(x)
		.orient("bottom")
		.tickFormat(d3.time.format("%d/%m/%Y %H:%M:%S"));

	var yAxis = d3.svg.axis()
		.scale(y)
		.orient("left");
		
	var line = d3.svg.line()
		.interpolate("basis")
		.x(function(d) { return x(d.date); })
		.y(function(d) { return y(d.sensor_value); });
		
	var svg = d3.select("#plotContainer").append("svg")
		.attr("width", width + margin.left + margin.right)
		//.attr("width", "100%")
		.attr("height", height + margin.top + margin.bottom)
		.append("g")
		.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

	
	
	x.domain([
		d3.min(data, function(c) { return d3.min(c.values, function(v) { return v.date; }); }),
		d3.max(data, function(c) { return d3.max(c.values, function(v) { return v.date; }); })
	  ]);

    y.domain([
		d3.min(data, function(c) { return d3.min(c.values, function(v) { return v.sensor_value; }); }),
		d3.max(data, function(c) { return d3.max(c.values, function(v) { return v.sensor_value; }); })
	  ]);
		
	svg.append("g")
		.attr("class", "x axis")
		.attr("transform", "translate(0," + height + ")")
		.call(xAxis)
		.selectAll("text")  
		.style("text-anchor", "end")
		.attr("dx", "-.8em")
		.attr("dy", ".15em")
		.attr("transform", function(d) {
			return "rotate(-55)" 
		});

    svg.append("g")
		.attr("class", "y axis")
		.call(yAxis)
		.append("text")
		.attr("transform", "rotate(-90)")
		.attr("y", -50)
		.attr("x", 20)
		.style("text-anchor", "end")
		.text("Value");

	  
	var fields = svg.selectAll(".field")
		.data(data);
			  
	var field = fields.enter().append("g")
		.attr("class", "field")
		.attr("id", function(d) { return d.name; });
	 
	var brush = d3.svg.brush()
      .x(x)
      .y(y)
      .on("brushend", brushend);
	 
	 field.append("path")
		.attr("class", "line")
		.attr("d", function(d) { return line(d.values); })
		.style({'fill': 'none','stroke-width':' 3px'})
		.style("stroke", function(d) { return color(d.name); });
	
	var legend = svg.selectAll(".legend")
		.data(color.domain().slice().reverse())
		.enter().append("g")
		.attr("class", "legend")
		.attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

	legend.append("rect")
		.attr("x", 300)
		.attr("y", 5)
		.attr("width", 15)
		.attr("height", 8)
		.style("fill", color);

	legend.append("text")
		.attr("x", 320)
		.attr("y", 10)
		.style("text-anchor", "start")
		.text(function(d) { return d; });
	
	fields.call(brush);

    function defineOnClickButton(){
        $("#plotBack").click(function(){

            if (typeof $("#plotBack") != 'undefined')  $("#plotBack").remove();
            if (currentTimeRangeShown == 0) {
                return;
            }


            currentTimeRangeShown--;

            if (currentTimeRangeShown > 0){
                $('#plotContainer').html('<input type="button" id="plotBack" value="Back" style="float: right;"/>');
                defineOnClickButton();
            } else {
                $('#plotContainer').html('');
            }

            var x1 = selectedTimeRangeHistory[currentTimeRangeShown].min;
            var x2 = selectedTimeRangeHistory[currentTimeRangeShown].max;
            selectedTimeRangeHistory[currentTimeRangeShown + 1] = null;
            var x1Month = x1.getMonth() + 1;
            var x2Month = x2.getMonth() + 1;

            $("#datepicker_from").val(x1.getDate() + "/" + x1Month +  "/" + x1.getFullYear() + " " + x1.getHours() + ":" + x1.getMinutes() + ":" + x1.getSeconds());
            $("#datepicker_to").val(x2.getDate() + "/" + x2Month +  "/" + x2.getFullYear() + " " + x2.getHours() + ":" + x2.getMinutes() + ":" + x2.getSeconds());

            var newData = [];
            for (var i=0;i<allData.length;i++){
                newData[i] = {name: allData[i].name, values: []};
                var cnt=0;
                for (var j=0;j<allData[i].values.length;j++){
                    if (x1<=allData[i].values[j].date && x2>=allData[i].values[j].date){
                        newData[i].values[cnt++] = allData[i].values[j]
                    }
                }
            }
            drawChartTimeseries(newData);
        });
    }

	function brushend(d) {
		var e = brush.extent();
		var x1=e[0][0];
		var x2=e[1][0];

        //update datetime fields
        var x1Month = x1.getMonth() + 1;
        var x2Month = x2.getMonth() + 1;

        $("#datepicker_from").val(x1.getDate() + "/" + x1Month +  "/" + x1.getFullYear() + " " + x1.getHours() + ":" + x1.getMinutes() + ":" + x1.getSeconds());
        $("#datepicker_to").val(x2.getDate() + "/" + x2Month +  "/" + x2.getFullYear() + " " + x2.getHours() + ":" + x2.getMinutes() + ":" + x2.getSeconds());


        if (typeof $("#plotBack") != 'undefined')  $("#plotBack").remove();
        $('#plotContainer').html('<input type="button" id="plotBack" value="Back" style="float: right;"/>');

        currentTimeRangeShown++;
        selectedTimeRangeHistory[currentTimeRangeShown] = {min: x1, max: x2};

        defineOnClickButton();


		var newData = [];
		for (var i=0;i<data.length;i++){
			newData[i] = {name: data[i].name, values: []};
			var cnt=0;
			for (var j=0;j<data[i].values.length;j++){
				if (x1<=data[i].values[j].date && x2>=data[i].values[j].date){
					newData[i].values[cnt++] = data[i].values[j]
				}
			}
		}
		drawChartTimeseries(newData);


	  }
}