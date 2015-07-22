var GsnPlots = {

  config: [],
  deployments: new Array(),
  gsnserver: "",
  setConfig : function (config) {
    $(config).each(function () {
      $(this.signals).each(function() {
        if (this.field.constructor != Array) {
          this.field = [this.field];
        }
      });
    });
    // array of { signals [{vsensor, field[], select, position, deviceId}], "title"},
    var a = document.createElement('a');
    a.href="/";
    if (a.host.match(/^pbl.permasense.*/) ||
    	a.host.match(/^croz.*:22080/) ||
    	a.host.match(/^croz.*:22443/) ||
    	a.host.match(/^data.permasense.ch:80/) ||
    	a.host.match(/^data.permasense.ch:22080/) ||
    	a.host.match(/^data.permasense.ch:22443/) ||
    	a.host.match(/^data.permasense.ch:443/) )
    	
      GsnPlots.gsnserver = "data.permasense.ch";
    else if (a.host.match(/^tpbl.permasense.*/) ||
    		 a.host.match(/^croz.*:23080/) ||
    		 a.host.match(/^croz.*:23433/) )
    	GsnPlots.gsnserver = "tpbl.permasense.ethz.ch";
    else
      GsnPlots.gsnserver = a.host;
    GsnPlots.config = GsnPlots.config.concat(config);
  },
    
  getDeployment: function (name) {
    var d=-1;
    for (i=0;i<GsnPlots.deployments.length;i++)
      if (GsnPlots.deployments[i].name==name) {
        d=GsnPlots.deployments[i];
        break;
      }
    return d;
  },
  
  getGraphConfigItem: function(graphconfig, index) {
    var item;
    $(graphconfig).each(function(){
      if (this.index == index)
        item = this;
    });
    if (item)
      return item;
    graphconfig.push({index:index ,signals: []});
    return graphconfig[graphconfig.length-1];
  },
  
  initPlots: function(gsnstructure, readycallback) {
    GsnPlots.readycallback = readycallback;
    $("virtual-sensor",gsnstructure).each(function(){
      var deploymentname = $(this).attr("name").split("_", 1)[0];
      if(deploymentname.length > 1) {
        deploymentname = deploymentname.substring(0, 1).toUpperCase() + deploymentname.substring(1, deploymentname.length).toLowerCase();
        if (GsnPlots.getDeployment(deploymentname) < 0) {
          GsnPlots.deployments.push(new Object({"name":deploymentname, "graphconfig":[], "topology":""}));
        }
        var thisdeployment = GsnPlots.getDeployment(deploymentname);
        var vsname = $(this).attr("name");
        var vs = this;
        // find out about available node ids/positions
        if(vsname.match(/.*_topology__public$/)) {
          thisdeployment.topology_vs = $(this).attr("name");$
        }
        // match configured plot vs
        $(GsnPlots.config).each( function(index) {
          $(this.signals).each(function() {
            if (vsname.match(this.vsensor)) {
              // create config if not available
              var graphconfigitem = GsnPlots.getGraphConfigItem(thisdeployment.graphconfig, index);
              // add vs
              var unit=Array();
              var field = this.field;
              $("field", vs).each(function() {
                var fieldpos = $.inArray($(this).attr("name"), field);
                if (fieldpos>-1) {
                  unit[fieldpos]=$(this).attr("unit");
                }
              });
              graphconfigitem.signals.push({vs: vsname, field:this.field, unit:unit, select:this.select, position:this.position, deviceId:this.deviceId, timeline: this.timeline});
            } 
          });
        });
      }
    });
    GsnPlots.readycallback();
  },
    
  getDeployments: function(rincl, rexcl) {
    if (!rincl && !rexcl)
      rincl=".*";
    var d = Array();
    $(GsnPlots.deployments).each(function() {
      if (this.name.match(rincl)) {
        if (!rexcl || !this.name.match(rexcl))
          d.push(this.name);
      }
    });
    return d;
  },
  
  addPlots : function(htmlelement, deployment) {
    htmlelement.empty();
    var d = GsnPlots.getDeployment(deployment);
    if (d == -1) {
      htmlelement.html("<p>No data available.</p>"); 
    }
    else {
      if (d.topology == false && d.topology_vs) {
        // fetch topology
         $.ajax({
           type: "GET",
           url: "/field?vs="+d.topology_vs+"&field=DATA&pk=latest",
           dataType: "xml",
          // ajax SUCCESS ***********************************
           success: function(data){
             d.topology=data;
             GsnPlots.addPlots(htmlelement, deployment);
           },
           error : function(XMLHttpRequest, textStatus, errorThrown) {
             d.topology="";
           }
         });
      }
      else {/*
        // add master timeslider
        var sliderwidth = 400;
        var sliderdiv = document.createElement('div');
        $(sliderdiv).css({'position':'fixed', 'bottom':'0px'});
        // Div for showing selected time span
        GsnPlots.sliderInfo = document.createElement('div');
        $(GsnPlots.sliderInfo).css({ 'width':'350px', 'height':'20px', 'position':'relative', 'top':'-18px', 'left':(sliderwidth-350)+'px', 'fontSize':'8pt', 'textAlign':'right'});
        sliderdiv.appendChild(GsnPlots.sliderInfo);
        // timeslider
        /*MasterTimeSlider.prototype = new TimeSlider;
        MasterTimeSlider.setMaxRange = function (start, end) {
          if (!this.fullDataMin)
            this.fullDataMin = start;
          this.fullDataMin = Math.min(this.fullDataMin, start);
          if (!this.fullDataMax)
            this.fullDataMax = end;
          this.fullDataMax = Math.max(this.fullDataMax, end);
          if (this.fullDataRange < this.minRangeDefault * 10)
            this.minRange = this.fullDataRange / 10;
          else
            this.minRange = this.minRangeDefault;
        }
        
        GsnPlots.mastertimeslider = new MasterTimeSlider({
           changedCallback : function(mindate, maxdate) {
             GsnPlots._masterChangedCallback(mindate, maxdate);
           },
           width : sliderwidth,
           infoHtmlElement: GsnPlots.sliderInfo
        });
        sliderdiv.appendChild(GsnPlots.mastertimeslider.getHtmlElement());
        $('body').append(sliderdiv);
        */
        d.graphconfig.sort(function(a,b){return a.index - b.index});
        // add plots
        $(d.graphconfig).each(function() {
          // this.config.masterslider = GsnPlots.mastertimeslider;
          var vizconfig = {
              "graph": {
                "width": 940,
                "height": 350,
                "showForm": true
              },
              "inittimerange": 864000000,
//              "inittimerange": 2592000000,
              "appUrl": "http://vizzly.ethz.ch/vizzly?",
              "signals": [],
          };
          $(this.signals).each(function() {
          // switch following cases:
          // 1) no selection criteria (missing select, position, deviceId)
          // 2) selection by topology properites (select)
          // 3) selection by position
          // 4) selection by deviceId            
            var thissignal=this;
            if (!this.select && !this.position && !this.deviceId) {                            
              $(this.field).each(function(index, value) {
                var signal = {
                  "displayName": this,
                  "dataSource": { "type": "gsn", "serverAddress": GsnPlots.gsnserver, "name": thissignal.vs },
				  "dataField": this,
                  "deviceSelect": { "type": "all" },
				  "timeField": thissignal.timeline?thissignal.timeline:"generation_time",
		          "scaling": 1.0,
                  "visible": true
                };
                if (typeof thissignal.unit[index] == "string")
                  signal.unit = thissignal.unit[index];
                vizconfig.signals.push(signal);      
              });
            }
            
            else if (this.select && this.select.length > 0) {
              var positions = new Array();
              $(thissignal.select,d.topology).each(function() {
                positions.push($(this).attr("position").valueOf());
              });
              positions.sort(function(a,b){return a - b});
              // add nodes to plot
              $(positions).each(function() {
                var p = this;
                $(thissignal.field).each(function(index, value) {
                  var signal = {
                    "displayName":"Position "+p +" "+this,
					"dataSource": { "type": "gsn", "serverAddress": GsnPlots.gsnserver, "name": thissignal.vs },
					"dataField": this,
	                "deviceSelect": { "type": "single", "field": "position", "value": p },
					"timeField": thissignal.timeline?thissignal.timeline:"generation_time",
			        "scaling": 1.0,
	                "visible": true
		          };
                  if (typeof thissignal.unit[index] == "string")
                    signal.unit = thissignal.unit[index];
                  vizconfig.signals.push(signal);      
                });
              });              
            }
            
            else if (this.position && this.position.length > 0) {
              // add nodes to plot
              $(this.position).each(function() {
                var p = this;
                $(thissignal.field).each(function(index, value) {
                  var signal = {
                    "displayName":"Position "+p +" "+this,
					"dataSource": { "type": "gsn", "serverAddress": GsnPlots.gsnserver, "name": thissignal.vs },
					"dataField": this,
	                "deviceSelect": { "type": "single", "field": "position", "value": p },
					"timeField": thissignal.timeline?thissignal.timeline:"generation_time",
			        "scaling": 1.0,
	                "visible": true
                  };
                  if (typeof thissignal.unit[index] == "string")
                    signal.unit = thissignal.unit[index];
                  vizconfig.signals.push(signal);      
                });
              });              
            }
           
            else if (this.deviceId && this.deviceId.length > 0) {
              // add nodes to plot
              $(this.deviceId).each(function() {
                var id = this;
                $(thissignal.field).each(function(index, value) {
                  var signal = {
                    "displayName":"Device "+id +" "+this,
					"dataSource": { "type": "gsn", "serverAddress": GsnPlots.gsnserver, "name": thissignal.vs },
					"dataField": this,
	                "deviceSelect": { "type": "single", "field": "device_id", "value": id },
					"timeField": thissignal.timeline?thissignal.timeline:"generation_time",
			        "scaling": 1.0,
	                "visible": true
                  };
                  if (typeof thissignal.unit[index] == "string")
                    signal.unit = thissignal.unit[index];
                  vizconfig.signals.push(signal);      
                });
              });              
            }

          });
          
          // finally add to html
          if (vizconfig.signals.length>0)
            GsnPlots._addPlotsAddGraph(htmlelement, vizconfig, GsnPlots.config[this.index].title);
        });
      }
    }
  },
  
  divid : 0,
  
  _addPlotsAddGraph: function(htmlelement, plot, title) {
    $(htmlelement).append('<div><h2>'+title+'</h2><div id="plot_'+GsnPlots.divid+'"/></div>');
    plot.graph.div='plot_'+(GsnPlots.divid++);
    var g = new LinePlotWidget(plot);
  },
  
  _masterChangedCallback: function() {
  }
  
};
