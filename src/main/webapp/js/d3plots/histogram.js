function include(filename)
{
    var head = document.getElementsByTagName('head')[0];
   
    script = document.createElement('script');
    script.src = filename;
    script.type = 'text/javascript';
   
    head.appendChild(script)
}

include("js/d3plots/d3.v3.min.js");

function drawHistogram(dataset, nb_elements){
	drawChartHistogram(parseDataHistogram(dataset, nb_elements),  d3.keys(dataset));
}

var selectedTimeRangeHistory = [];
var currentTimeRangeShown = 0;

function parseDataHistogram(dataset, nb_elements){
	var keys = d3.keys(dataset);
	var data_array = [];
    var cnt = 0;
    var fields = 0;
    for (var i=0; i<keys.length; i++){
        if (keys[i].indexOf("aggregation_interval") > -1) continue;
        if (fields == 1) {
            $("#plotContainer").html('</br><span style="color:red">Please select only one field for histogram!</span>');
            return -1;
        }
        for (var j=0; j<dataset[keys[i]].data.length; j++){
            if (isNaN((dataset[keys[i]].data[j])[1])){
                continue;
            }
            data_array[cnt++]  = (dataset[keys[i]].data[j])[1];
        }
        fields = 1;
    }

    var histogram = d3.layout.histogram();
    selectedTimeRangeHistory[0] = [];
    selectedTimeRangeHistory[0]['data'] = histogram(data_array);
    selectedTimeRangeHistory[0]['min'] = $(".min").val();
    selectedTimeRangeHistory[0]['max'] = $(".max").val();
    selectedTimeRangeHistory[0]['slider_range'] =  $( ".slider-range" ).slider( "values");
	return selectedTimeRangeHistory[0]['data'];
}

function drawChartHistogram(data, f1fullname){
	if (data == -1) return;
	var margin = {top: 40, right: 60, bottom: 50, left: 80},
		width = 700 - margin.left - margin.right,
		height = 300 - margin.top - margin.bottom;	
	
	var x = d3.scale.ordinal()
		.domain(data.map(function(d) { return d.x+d.dx/2; }))
		.rangeRoundBands([0, width], .1);

	var yMaxDomain = d3.max(data, function(c) { return c.y; });
	if (0 == yMaxDomain) {
		yMaxDomain = 1;
	}
	var y = d3.scale.linear()
		.domain([0, yMaxDomain])
		.range([height, 0]);

	var xAxis = d3.svg.axis()
		.scale(x)
		.orient("bottom")
		.tickFormat(d3.format(".2f"));

	var yAxis = d3.svg.axis()
		.scale(y)
		.orient("left")
		.ticks(yMaxDomain > 10 ? 10 : yMaxDomain);
	
	//svg
	var svg = d3.select("div#plotContainer")
		.append("svg")
		.attr("width", width + margin.left + margin.right)
		.attr("height", height + margin.top + margin.bottom)
		.append("g")
		.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
		
	var synchronizedMouseOver = function() {
		var bar = d3.select(this);
		var indexValue = bar.attr("index_value");

		var barSelector = "rect.-bar-" + indexValue;
		var selectedBar = d3.selectAll(barSelector);
		selectedBar.style("fill", "#E0EEE0");
	};

	var synchronizedMouseOut = function() {
		var bar = d3.select(this);
		var indexValue = bar.attr("index_value");

		var barSelector = "rect.-bar-" + indexValue;
		var selectedBar = d3.selectAll(barSelector);
		selectedBar.style("fill", "#77cc66");
	};

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



            var histogram = d3.layout.histogram();
            var d = selectedTimeRangeHistory[currentTimeRangeShown]['data'];
            drawChartHistogram(d,  f1fullname);

            selectedTimeRangeHistory[currentTimeRangeShown + 1] = null;

            //update range
            $(".min").val(selectedTimeRangeHistory[currentTimeRangeShown]['min']);
            $(".max").val(selectedTimeRangeHistory[currentTimeRangeShown]['max']);
            $( ".slider-range" ).slider( "values", selectedTimeRangeHistory[currentTimeRangeShown]['slider_range'] );
        });
    }
	  
	var synchronizedMouseClick = function(d,i) {
        if (typeof $("#plotBack") != 'undefined')  $("#plotBack").remove();
        $('#plotContainer').html('<input type="button" id="plotBack" value="Back" style="float: right;"/>');

        var histogram = d3.layout.histogram();
        var dataset = histogram(d);
		drawChartHistogram(dataset,  f1fullname);

        currentTimeRangeShown++;
        selectedTimeRangeHistory[currentTimeRangeShown] = [];
        selectedTimeRangeHistory[currentTimeRangeShown]['data'] = dataset;
        selectedTimeRangeHistory[currentTimeRangeShown]['min'] = d.x;
        selectedTimeRangeHistory[currentTimeRangeShown]['max'] = d.x + d.dx;
        selectedTimeRangeHistory[currentTimeRangeShown]['slider_range'] = [ d.x, d.x + d.dx ];

        defineOnClickButton();

        //update range
        $(".min").val(d.x);
        $(".max").val(d.x + d.dx);
        $( ".slider-range" ).slider( "values", [ d.x, d.x + d.dx ] );
      };
	

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
        })
		.append("text")
		.attr("x", width)
		.attr("y", 30)
        .style("text-anchor", "end")
		.text("Values");

	//y axis
    svg.append("g")
		.attr("class", "y axis")
		.call(yAxis)
		.append("text")
		.attr("transform", "rotate(-90)")
		.attr("y", -50)
		.attr("x", 26)
		.style("text-anchor", "end")
		.text("Frequency");
	
	svg.append("g")
		.selectAll(".bars")
		.data(data)
		.enter()
		.append("rect")
        .attr("width", x.rangeBand())
		.attr("x", function(d) { return x(d.x+d.dx/2); })
		.attr("y", function(d) { return y(d.y); })
		.attr("height", function(d) { return y.range()[0] - y(d.y); })
        .attr("fill", "#77cc66")
		.on('mouseover', synchronizedMouseOver)
        .on("mouseout", synchronizedMouseOut)
		.on("click", synchronizedMouseClick)
		.attr("class", function(d, i) { return "-bar-index-" + i; })
        .attr("index_value", function(d, i) { return "index-" + i; })
		.order();
}