var GsnPlots = {

  config: false,
  deployments: new Array(),
  gsnserver: "",
  showstatistics : false,
  setConfig : function (config) {
    GsnPlots.config = config;
    $(GsnPlots.config).each(function () {
      if (this.field.constructor != Array) {
        this.field = [this.field];
      }
    });
    // array of {"vsensor", "field", "title", "select"},
    var a = document.createElement('a');
    a.href="/";
    if (a.host.match(/tik41x.*:22001/))
      GsnPlots.gsnserver = "data.permasense.ch";
    else
      GsnPlots.gsnserver = a.host;
  },
  
  setConfigStatistics : function (config) {
    GsnPlots.showstatistics = true;
    GsnPlots.setConfig(config);
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
  
  initPlots: function(gsnstructure, readycallback) {
    GsnPlots.readycallback = readycallback;
    $("virtual-sensor",gsnstructure).each(function(){
      var deploymentname = $(this).attr("name").split("_", 1)[0];
      if(deploymentname.length > 1 && (GsnPlots.showstatistics && deploymentname=='statistics' || !GsnPlots.showstatistics && deploymentname!='statistics')) {
        deploymentname = deploymentname.substring(0, 1).toUpperCase() + deploymentname.substring(1, deploymentname.length).toLowerCase();
        if (GsnPlots.getDeployment(deploymentname)<0) {
          GsnPlots.deployments.push(new Object({"name":deploymentname, "graphconfig":[], "topology":false}));
        }
        var thisdeployment = GsnPlots.getDeployment(deploymentname);
        var vsname = $(this).attr("name");
        var vs = this;
        // find out about available node ids/positions
        if(vsname.match(/.*_topology__public$/)) {
          thisdeployment.topology_vs = $(this).attr("name");$
        }
        // match configured plot vs
        $(GsnPlots.config).each( function() {
           if (thisdeployment.name.toLowerCase()+"_"+this.vsensor == vsname) {
             // add graphconfig
             var graphconfig = {
              "graph": {
                "type": "dygraph",
                "width": 940,
                "height": 350,
                "showForm": true
              },
              "inittimerange": 2592000000,
              "appUrl": "http://whymper.ethz.ch:24001/sensorviz?",
              "signals": {
                "y1": []
              },
             };
             this.unit=Array();
             var meta = this;
             $("field", vs).each(function() {
               var fieldpos = $.inArray($(this).attr("name"), meta.field);
               if (fieldpos>-1) {
                 meta.unit[fieldpos]=$(this).attr("unit");
               }
             });
             thisdeployment.graphconfig.push({"config":graphconfig, "meta":this});
           }
        }); 
      }
    });
    GsnPlots.readycallback();
  },
    
  getDeployments: function() {
    var d = Array();
    $(GsnPlots.deployments).each(function() {
      d.push(this.name);
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
      if (d.topology == false && GsnPlots.showstatistics!=true) {
        // fetch topology
         if (d.topology_vs)
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
         else 
           d.topology="";
      }
      else {
        $(d.graphconfig).each(function() {
          if (GsnPlots.showstatistics==true) {
            var vsensor = d.name.toLowerCase()+"_"+this.meta.vsensor;
            var graphsignals = this.config.signals.y1;
            var meta = this.meta;
            $(this.meta.field).each(function(index, value) {
              var signal = {
                "displayName": this,
                "gsnUrl": GsnPlots.gsnserver,
                "virtualSensor": vsensor,
                "field": this,
                "scaling": 1.0,
                "visible": true,
                "timeline":"timed"
              };
              if (typeof meta.unit[index] == "string")
                signal.unit = meta.unit[index];
              graphsignals.push(signal);              
            });
          }
          else {
            this.config.signals.y1 = [];
            var positions = new Array();
            $(this.meta.select,d.topology).each(function() {
              positions.push($(this).attr("position").valueOf());
            });
            positions.sort(function(a,b){return a - b});
            var graphconfig = this;
            // add nodes to plot
            $(positions).each(function() {
              var p = this;
              $(graphconfig.meta.field).each(function(index, value) {
                var signal = {
                  "displayName":"Position "+p +" "+this,
                  "gsnUrl": GsnPlots.gsnserver,
                  "virtualSensor": d.name.toLowerCase()+"_"+graphconfig.meta.vsensor,
                  "position": p,
                  "field": this,
                  "scaling": 1.0,
                  "visible": true,
                  "timeline":"generation_time"
                };
                if (typeof graphconfig.meta.unit[index] == "string")
                  signal.unit = graphconfig.meta.unit[index];
                graphconfig.config.signals.y1.push(signal);
              });
            }); 
          }
          // add div and graph
          GsnPlots._addPlotsAddGraph(htmlelement, this);
        });
      }
    }
  },
  
  divid : 0,
  
  _addPlotsAddGraph: function(htmlelement, plot) {
    $(htmlelement).append('<div><h2>'+plot.meta.title+'</h2><div id="plot_'+GsnPlots.divid+'"/></div>');
    plot.config.graph.div='plot_'+(GsnPlots.divid++);
    var g = new FrontendCreator(plot.config);
  }
  
};
