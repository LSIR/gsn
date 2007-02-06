/**
 * gsn javascript
 */
var map;
 
var GSN = { 
	debugmode: false
	,debug: function (txt) {
		if(typeof console != "undefined" && this.debugmode) {
			console.debug(txt);
		}	
	}
	,context: null //home, data, map || fullmap
	,msg: null  //if there is some msg to give back
	/**
	* Initialize a page load (begin, tab click & back button)
	*/
	,load: function(){
		//by default, load home
		if (location.hash == "") location.hash = "home";
		
		GSN.debug("init:"+location.hash);
		var params=location.hash.substr(1).split(",");
		
		GSN.context = params[0];
		//highlight the right tab in the navigation bar
		$("#navigation li").each(function(){
			if($("a",this).text()==GSN.context)
				$(this).addClass("selected");
			else
				$(this).removeClass("selected");
		});
		
		//take care of msg params
		for (var i=1;i<params.length;i++){
			val = params[i].split("=");
			if (val[0]=="msg") {
				GSN.msg = val[1]
				params.splice(i,1);
				$.historyLoad(params.join(","));
				return;
			}
		}		
		
		$("#main > div").hide();
		if (GSN.context!="map") {
			$("#toggleallmarkers").hide();
			$("#vsmenu .toggle").hide();			
		}
		//for each page context
		if (GSN.context=="home")	{
			GSN.vsbox.container = "#vs";
			$("#main #control").show();
			$(".msg").hide();
			$(".intro").show();
			$("#main #homediv").show();
			$("#control #closeall").show();
			//load and display all the visual sensors
			if (!GSN.loaded) GSN.updateall();
			
			if (GSN.msg != null) {
				$(".msg").show();
				$(".intro").hide();
				if (GSN.msg=="upsucc")
					$("#control .msg").empty().append("The upload to the virtual sensor went successfully!");
				GSN.msg = null;
			}
		} else if (GSN.context=="data")	{
			$(".msg").remove();
			$("#main #datachooser").show();
			if (!GSN.loaded) GSN.updateall();
		} else if (GSN.context=="map")	{
			GSN.vsbox.container = "#vs4map";
			$(".intro").hide();
			$(".msg").remove();
			$("#main #control").show();
			$("#control #closeall").hide();
			$("#main #mapdiv").show();
			$("#toggleallmarkers").show();
			$("#vsmenu .toggle").show();
			if(!GSN.map.loaded) {
				$("#refreshall_autozoomandcenter").attr("checked",true);
				GSN.map.init();
				GSN.updateall();
			}
			
			//take care of params
			if (params.length>1) {
				var lat=lng=zoom=null;
				for (var i=1;i<params.length;i++){
					val = params[i].split("=");
					if (val[0]=="lt") lat = val[1];
					if (val[0]=="lo") lng = val[1];
					if (val[0]=="z") zoom = parseInt(val[1]);
				}
				if (lat!=null) {
					map.setCenter(new GLatLng(lat,lng),zoom);
					$("#refreshall_autozoomandcenter").attr("checked",false);
				}
			}
			GSN.map.autozoomandcenter();
		} else if (GSN.context=="fullmap")	{
			GSN.vsbox.container = "#vs";
			GSN.map.followMarker(null);
			if(!GSN.map.loaded) {
				GSN.map.init();
				GSN.updateall();
			}
		}
	}
	/**
	* Click on the top navigation bar
	*/
	,nav: function (page) {
		$.historyLoad(page);
		return false;
	}
	/**
	* Click on the virtual sensor on the left bar
	*/
	,menu: function (vsName) {
		$(".intro").remove();
		//define the click depending the context (home,data,map)
		if (GSN.context=="home"){
			GSN.addandupdate(vsName);
		} else if (GSN.context=="map"){
			var prev;
			if ($("#vs4map div").size()!=0)
				prev = $("#vs4map div").attr("class").split(" ")[0].split("-")[1];
			
			if (prev != vsName) {
				$("#vs4map").empty();
				GSN.map.followMarker(vsName);
				GSN.addandupdate(vsName);
			} else
				GSN.vsbox.remove(vsName);
		} else if (GSN.context=="data"){
			GSN.data.init(vsName);
		} else if (GSN.context=="fullmap"){
			$(".vsbox").removeClass("followed");
			$(".vsbox-"+vsName).addClass("followed");
			GSN.map.followMarker(vsName);
			GSN.map.autozoomandcenter();
		}
	}
	/**
	* Close all button
	*/
	,closeall: function (){
		$(".intro").remove();
		$("#vs").empty();
		GSN.map.followMarker(null);
	}
	,loaded : false
	/**
	* Initialize the gsn title and leftside menu
	*/
	,init : function(data) {
		this.loaded=true;
		$(".loading").remove();
		
		//show all the gsn container info
		if ($(document).attr("title")=="GSN") {
			var gsn = $("gsn",data);
			$(document).attr("title",$(gsn).attr("name")+" :: GSN");
			$("#gsn-name").empty().append($(gsn).attr("name")+" :: GSN");
			$("#gsn-desc").empty().append($(gsn).attr("description"));
			$("#gsn-author").empty().append($(gsn).attr("author")+" ("+$(gsn).attr("email")+")");
		}
		//build the leftside vs menu
		$("#vsmenu").empty();
		$("virtual-sensor",data).each(function(){
			var vsname = $(this).attr("name");
			$("#vsmenu").append($.LI({},$.A({"href":"javascript:GSN.menu('"+vsname+"');","id":"menu-"+vsname+""},vsname)));
			//if ($("field[@name=latitude]",$(this)).text()!="") 
			//	$("#menu-"+vsname).addClass("gpsenabled");
		});
	}
	,updatenb: 0
	,updateallchange: function(){
		if($("#refreshall_timeout").attr("value") != 0)
			GSN.updateall();
	}
	/**
	* Ajax call to update all the sensor display on the page and the map
	*/
	,updateall: function(num,showall){
		//to prevent multiple update instance
		if (typeof num == "number" && num != GSN.updatenb) return;
		GSN.updatenb++;
		
		$(".refreshing").show();
		
		var firstload = !GSN.loaded;
  		
		$.ajax({ type: "GET", url: "/gsn", success: function(data){
			var start = new Date();
			//initalisation of gsn info, vsmenu
			if (!GSN.loaded) GSN.init(data);
			
			//create vsbox on the first load
			if (firstload && GSN.context == "home") {
				for (var i = 0; i < 10; ++i){
					var n = $($("virtual-sensor",data).get(i)).attr("name");
					if (n!=null) GSN.vsbox.add(n);
				}
			} else if (firstload && GSN.context == "fullmap") {
				$("virtual-sensor",data).each(function(){
					GSN.vsbox.add($(this).attr("name"));
				});
			}
			
			//update vsbox
			$("virtual-sensor",data).each(function(){
				GSN.vsbox.update(this);
			});
			
			//update map
			GSN.map.autozoomandcenter();
			
			//next refresh
			if($("#refreshall_timeout").attr("value") > 0)
				setTimeout("GSN.updateall("+GSN.updatenb+")", $("#refreshall_timeout").attr("value"));
			
			$(".refreshing").hide();	
			
			var diff = new Date() - start;
			GSN.debug("updateall time:"+diff/1000); 
		}});
	}
	/**
	* Add a vsbox if it doesn't exist, bring it to front and update it
	*/
	,addandupdate: function(vsName){
		GSN.vsbox.bringToFront(vsName);
		$.ajax({ type: "GET", url: "/gsn?name="+vsName, success: function(data){
			$("virtual-sensor[@name="+vsName+"]",data).each(function(){
					GSN.vsbox.update(this);
			});
			
			//update map
			GSN.map.autozoomandcenter();
		}});
	}
	/**
	* vsbox, display the vs info
	*/
	,vsbox: {
		//box showing all vs info
		container: "#vs"
		/**
		* Create an empty vsbox
		*/
		,add: function(vsName) {
			var vsdiv = "vsbox-"+vsName;
			if ($("."+vsdiv, $(this.container)).size()!=0) return; //already exists
			$(this.container).append($.DIV({"class":vsdiv+" vsbox"},
									  $.H3({},$.SPAN({"class":"vsname"},vsName),
									  	$.A({"href":"javascript:GSN.vsbox.remove('"+vsName+"');","class":"close"},"close"),
								      	$.SPAN({"class":"timed"},"loading...")
									    ),$.UL({"class":"tabnav"},
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','dynamic');","class":"tabdynamic active"},"dynamic")),
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','static');","class":"tabstatic"},"addressing")),
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','structure');","class":"tabstructure"},"structure")),
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','description');","class":"tabdescription"},"description")),
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','upload');","class":"tabupload"},"upload"))									    	
									      ),
									  $.DL({"class":"dynamic"}),
									  $.DL({"class":"static"}),
									  $.DL({"class":"structure"}),
									  $.DL({"class":"description"}),
									  $.DL({"class":"upload"},
									  	$.FORM({"action":"/upload","method":"post","enctype":"multipart/form-data"},
									  		$.INPUT({"type":"hidden","name":"vsname","value":vsName}),
									  		$.SELECT({"class":"cmd","name":"cmd"}),
									  		$.DL({"class":"input"}),
									  		$.INPUT({"type":"submit","value":"upload"}),
									  		$.P({},"* compulsary fields.")
									  	)
									  )
			));
			$(this.container).find("."+vsdiv+" select.cmd").bind("change", function(event) {GSN.vsbox.toggleWebInput(event)});
		}
		/**
		* Bring a vsbox at the beginning of the container
		*/
		,bringToFront: function(vsName) {
			this.add(vsName);
			var vsdiv = "vsbox-"+vsName;
			$("."+vsdiv, $(this.container)).hide();
			$(this.container).prepend($("."+vsdiv, $(this.container)));
			$("."+vsdiv, $(this.container)).fadeIn("slow");
		}
		/**
		* Update and show all the data of the vsbox
		*/
		,update: function (vs){

			//when map is enable, update marker
			if (GSN.map.loaded){
				var lat = $("field[@name=latitude]",vs).text();
				var lon = $("field[@name=longitude]",vs).text();
				if (lat != "" && lon != ""){
					GSN.map.updateMarker($(vs).attr("name"),lat,lon);
				}
			}
			
			//update the vsbox
			var vsd = $(".vsbox-"+$(vs).attr("name"), $(this.container))[0];
			if (typeof vsd == "undefined") return;
			//if (vsd.css("display")=="none") return;
			
			var vsdl = $("dl", vsd);
			var dynamic = vsdl.get(0);
			var static = vsdl.get(1);
			var struct = vsdl.get(2);
			var input = $("dl.input",vsdl.get(4));
			dl = dynamic;
			
			var name,cat,type,value;
			var last_cmd,cmd;
			var hiddenclass ="";
			//update the vsbox the first time, when it's empty
			if ($(dynamic).children().size()==0 && $(static).children().size()==0){
			  var gotDynamic,gotStatic,gotInput = false;
			  $("field",vs).each(function(){ 
				name = $(this).attr("name");
				cat = $(this).attr("category");
				cmd = $(this).attr("command");
				type = $(this).attr("type");
				value = $(this).text();
				
				if (name=="timed") {
			  		if (value != "") value = GSN.util.printDate(value);
					$("span.timed", vsd).empty().append(value);
			  		return;
			  	}
				
				if (cat=="input") {
					dl = input;
					if (!gotInput) {	
						$("a.tabupload", vsd).show();
						gotInput = true;
					}
				} else if (cat=="predicate") {
					dl = static;
					if (!gotStatic) {	
						$("a.tabstatic", vsd).show();
			  			if (!gotDynamic) {
			  				$("a.tabstatic", vsd).addClass("active");
			  				$("dl", vsd).hide();
			  				$("dl.static", vsd).show();
			  			}
						gotStatic = true;
					}
				} else {
					//add to structure
					var s = type ;
					if ($(this).attr("description")!=null)
						s += ' <img src="style/help_icon.gif" alt="" title="'+$(this).attr("description")+'"/>';	
					$(struct).append('<dt>'+name+'</dt><dd class="'+name+'">'+s+'</dd>');
					if (!gotDynamic) {
			  			$("a.tabdynamic", vsd).show();
						$("a.tabstructure", vsd).show();
						gotDynamic = true;
					}
				}
							
				//set the value
				if (cat == null) {
					if (value == "") {
						value = "null";
					} else if (type.indexOf("svg") != -1){
						value = '<embed type="image/svg+xml" width="400" height="400" src="'+value+'" PLUGINSPAGE="http://www.adobe.com/svg/viewer/install/" />';
					} else if (type.indexOf("image") != -1){
						value = '<img src="'+value+'" alt="error" />';
					} else if (type.indexOf("binary") != -1){
						value = '<a href="'+value+'">download <img src="style/download_arrow.gif" alt="" /></a>';
					}
				} else if (cat == "input") {
					if (last_cmd != cmd) {
						if (last_cmd != null) hiddenclass = ' hidden';
						$("select.cmd", vsd).append($.OPTION({},cmd));
						last_cmd = cmd;
					}
					var comp = '';
					if (type.substr(0,1)=="*") {
						comp = '*';
						type=type.substr(1);
					}

					if (type.split(":")[0].indexOf("binary") != -1){
						value = '<input type="file" name="'+cmd+";"+name+'"/>';
					} else if (type.split(":")[0].indexOf("select") != -1){
						var options = type.split(":")[1].split("|");
						value = '<select name="'+cmd+";"+name+'">';
						for (var i = 0; i < options.length;i++){
							value += '<option>'+options[i]+'</option>';
						}
						value += '</select>';						
					}  else if (type.split(":")[0].indexOf("radio") != -1 ||
								type.split(":")[0].indexOf("checkbox") != -1){
						var options = type.split(":")[1].split("|");
						value = '';
						for (var i = 0; i < options.length;i++){
							value += '<input type="'+type.split(":")[0]+'" name="'+cmd+";"+name+'" value="'+options[i]+'">'+options[i]+'</input>';
						}
					} else {
						value = '<input type="file" name="'+cmd+";"+name+'"/>';
					}
					if ($(this).attr("description")!=null)
						value += ' <img src="style/help_icon.gif" alt="" title="'+$(this).attr("description")+'"/>';	
					
					name = comp+name;
				} 
				$(dl).append('<dt class="'+cmd+hiddenclass+'">'+name+'</dt><dd class="'+name+((cmd!=null)?' '+cmd:'')+hiddenclass+'">'+value+'</dd>');				
			  });
			  
			  if ($(vs).attr("description")!="") {
			 	$("dl.description", vsd).append($.DD({},$(vs).attr("description")));
			 	$("a.tabdescription", vsd).show();
			 	if (!gotStatic) {
			  		$("a.tabdescription", vsd).addClass("active");
			  		$("dl", vsd).hide();
			  		$("dl.description", vsd).show();
			  	}
				
			  }
			  return true;
			} else {
				//update the vsbox when the value already exists
				var dds = $("dd",dl);
				var dd,field;
				for (var i = 0; i<dds.size();i++){
					dd = dds.get(i);
					field = $("field[@name="+$(dd).attr("class")+"]",vs);
					type = $(field).attr("type");
					value = $(field).text();
					if (value!="") {
						if (type.indexOf("svg") != -1){
							$("embed",dd).attr("src",value);
						} else if (type.indexOf("image") != -1){
							$("img",dd).attr("src",value);
						} else if (type.indexOf("binary") != -1){
							$("a",dd).attr("href",value);
						} else {
							$(dd).empty().append(value);
						}
					}
				}
				value = $("field[@name=timed]",vs).text();
				if (value != "") value = GSN.util.printDate(value);
				$("span.timed", vsd).empty().append(value);
				return false;
			}
		}
		/**
		* Remove the vsbox from the container
		*/
		,remove: function (vsName) {
			var vsdiv = "vsbox-"+vsName;
			$("."+vsdiv, $(this.container)).remove();
			GSN.map.followMarker(null);
			GSN.map.autozoomandcenter();
		}
		/**
		* Vsbox tabs control
		*/
		,toggle: function (vsName,dl){
			var vsdiv = "vsbox-"+vsName;
			$("."+vsdiv+" > dl", $(this.container)).hide();
			$("."+vsdiv+" > dl."+dl, $(this.container)).show();
			$("."+vsdiv+" a", $(this.container)).removeClass("active");
			$("."+vsdiv+" a.tab"+dl, $(this.container)).addClass("active");
		}
		,toggleWebInput: function (event){
			var cmd = event.target.options[event.target.selectedIndex].text;
			$(event.target).parent().find("dt").hide();
			$(event.target).parent().find("dd").hide();
			$(event.target).parent().find("dt."+cmd).show();
			$(event.target).parent().find("dd."+cmd).show();
		}
	},
	/**
	* All the map thing
	*/
	map: {
		loaded: false //the #vsmap div is initialized
		,tinyred: null
		,tinygreen: null
		,markers : new Array()
		,highlighted : null
		,highlightedmarker : null
		/**
		* Initialize the google map
		*/
		,init : function(){
			if(typeof GBrowserIsCompatible == "undefined") {
				//no internet
				$("#vsmap").empty().append($.P({"class":"error"},"Google maps isn't loaded! Maybe your internet connection is not working."));
			} else if(!GBrowserIsCompatible()) {
				//bad api key
				$("#vsmap").empty().append($.P({"class":"error"},"Your browser isn't compatible to Google maps or the Google maps API key is wrong. By default, Google maps only works if your using the host : http://localhost:22001/ . If you need a different host, edit index.html and change the google maps API key."));
			} else if (GBrowserIsCompatible()) {
				//load and initialize google map
				GSN.debug("init gmap");
       
       			this.loaded=true;
				
        		map = new GMap2(document.getElementById("vsmap"));
        		//set the different control on the map
        		map.addControl(new GLargeMapControl());
				map.addControl(new GMapTypeControl());
				map.addControl(new GScaleControl());
				map.addControl(new GOverviewMapControl());

				/*
				// custom epfl map
				var copyright = new GCopyright(1, new GLatLngBounds(new GLatLng(-90, -180), new GLatLng(90, 180)), 16, "©2006 EPFL");
				var copyrightCollection = new GCopyrightCollection('Imagery');
				copyrightCollection.addCopyright(copyright);
				var tileLayers = [new GTileLayer(copyrightCollection, 16, 17)];
				// retrieve the tiles location
				customGetTileUrl = function(a, b) {
					return "http://sensorscope.epfl.ch/map/image/" + a.x + "_" + a.y + "_" + (17 - b) + ".jpg"
				}
				tileLayers[0].getTileUrl = customGetTileUrl;
				// display the custom map
				var customMap = new GMapType(tileLayers, new GMercatorProjection(18), "Aerial", {errorMessage:"Aerial imagery unavailable."});
				map.addMapType(customMap);
				*/			


				// Create our "tiny" markers icon
				var tinyred = new GIcon();
				tinyred.image = "http://labs.google.com/ridefinder/images/mm_20_red.png";
				tinyred.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
				tinyred.iconSize = new GSize(12, 20);
				tinyred.shadowSize = new GSize(22, 20);
				tinyred.iconAnchor = new GPoint(6, 20);
				tinyred.infoWindowAnchor = new GPoint(5, 1);
				GSN.map.tinyred = tinyred;
				var tinygreen = new GIcon();
				tinygreen.image = "http://labs.google.com/ridefinder/images/mm_20_green.png";
				tinygreen.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
				tinygreen.iconSize = new GSize(12, 20);
				tinygreen.shadowSize = new GSize(22, 20);
				tinygreen.iconAnchor = new GPoint(6, 40);
				tinygreen.infoWindowAnchor = new GPoint(5, 1);
				GSN.map.tinygreen = tinygreen;
		
  				//attach event
  				GEvent.addListener(map, "click", function(overlay, point) {
					if(overlay)	{
						//when a marker is clicked
						if(typeof overlay.vsname != "undefined") 
							GSN.menu(overlay.vsname);
					}
					else
						$("#refreshall_autozoomandcenter").attr("checked",false);
				});		
				GEvent.addListener(map, 'zoomend', function (oldzoomlevel,newzoomlevel) {
  					GSN.map.zoomend(oldzoomlevel,newzoomlevel);
  					GSN.map.userchange();
				});
				
				GEvent.addListener(map, 'dragstart', function () {
  					$("#refreshall_autozoomandcenter").attr("checked",false);
				}); 
				
				GEvent.addListener(map, 'moveend', function () {
  					GSN.map.userchange();
				}); 
				
				map.setCenter(new GLatLng(0,0),1);
				
   			}
		}
		/**
		* Callback after any map change zoom and map move and vs toggle
		* Used for location #hash change
		*/	
		,userchange : function(){
			if (location.hash.substr(1,3)!="map") return;
  			var vs = (location.hash+",vs=[ALL],").split("vs=")[1].split(",")[0];			
  			if (!$("#refreshall_autozoomandcenter").attr("checked")) 
				location.hash = "map"+",lt="+map.getCenter().lat()+",lo="+map.getCenter().lng()+",z="+map.getZoom();
			else
				location.hash = "map"
			if (vs!="[ALL]") location.hash += ",vs="+vs;
		}
		/**
		* Callback after any zoom change
		* Used for the tricked followed marker
		*/	
		,zoomend : function(oldzoomlevel,newzoomlevel){
			GSN.map.trickhighlighted();
		}
		/**
		* Followed marker and top of the others
		*/
		,trickhighlighted : function(){
			if (GSN.map.highlighted != null) {
				var hPoint = map.getCurrentMapType().getProjection().fromLatLngToPixel(GSN.map.markers[GSN.map.highlighted].getPoint(),map.getZoom());
    			var marker = new GMarker(map.getCurrentMapType().getProjection().fromPixelToLatLng(new GPoint(hPoint.x , hPoint.y + 20 ) , map.getZoom()),GSN.map.tinygreen);
    			map.removeOverlay(GSN.map.highlightedmarker);
    			GSN.map.highlightedmarker = marker;
  				map.addOverlay(marker);
      		}
		}
		/**
		* Auto-zoom and center on the visible sensors
		*/
		,autozoomandcenter:function (){
			if (GSN.map.loaded && $("#refreshall_autozoomandcenter").attr("checked")){
				//not following any sensor
				if (GSN.map.highlighted!=null)
					map.panTo(GSN.map.markers[GSN.map.highlighted].getPoint());	
				else
					GSN.map.showAllMarkers();
			}
			if (GSN.map.loaded) {
				for (x in GSN.map.markers) {
					var m = GSN.map.markers[x];
					if (GSN.map.markerIsVisible(m.vsname)) {
						$("#menu-"+m.vsname).next().html("X");
						m.show();
					}
					else {
						$("#menu-"+m.vsname).next().html("O");
						m.hide();
					}
				}
			}
		}
		/**
		* Add marker
		*/
		,addMarker: function(vsName,lat,lon){
			if (!map.isLoaded())
				map.setCenter(new GLatLng(lat,lon), 13);
		
			var marker = new GMarker(new GLatLng(lat,lon),GSN.map.tinyred);
  			marker.vsname = vsName;
  			GSN.map.markers.push(marker);
  			map.addOverlay(marker);
  					
  			//add gpsenable classjaj
  			$("#menu-"+vsName).addClass("gpsenabled");
  			$("#menu-"+vsName).parent().append($.A({"href":"javascript:GSN.map.toggleMarker('"+vsName+"');","class":"toggle"},"X"));
  			
  			if(GSN.context=="fullmap"){
				var vs = $(".vsbox-"+vsName+" > h3 > span.vsname")
				$(vs).wrap("<a href=\"javascript:GSN.menu('"+$(vs).text()+"');\"></a>");
			}
		}
		/**
		* Toggle marker
		*/
		,markerIsVisible: function(vsName){
			var vs = (location.hash+",vs=[ALL],").split("vs=")[1].split(",")[0];
			if ((":"+vs+":").indexOf(":"+vsName+":")!=-1 || vs == "[ALL]") 
				return true;
			else
				return false;
		}
		/**
		* Toggle marker
		*/
		,toggleAllMarkers: function(){
			GSN.debug("in:toggleAllMarkers")
			var params=location.hash.substr(1).split(",");
			for (var i=1;i<params.length;i++){
				val = params[i].split("=");
				if (val[0]=="vs") {
					params.splice(i,1);
					$.historyLoad(params.join(","));
					return;
				}
			}
			params.push("vs=");
			$.historyLoad(params.join(","));
		}
		/**
		* Toggle marker
		*/
		,toggleMarker: function(vsName){
			var params=location.hash.substr(1).split(",");
			for (var i=1;i<params.length;i++){
				val = params[i].split("=");
				if (val[0]=="vs") {
					var vs = val[1].split(":");
					for (j in vs) {
						if (vs[j]==vsName) {
							vs.splice(j,1);
							params[i]="vs="+vs.join(":");
							$.historyLoad(params.join(","));
							return;
						}
					}
					if (vs[0]=="") vs = new Array();
					vs.push(vsName);
					if (vs.length<GSN.map.markers.length)
						params[i]="vs="+vs.join(":");
					else
						params.splice(i,1);
					$.historyLoad(params.join(","));
					return;
				}
			}
			var vs = new Array();
			for (x in GSN.map.markers) {
				if (GSN.map.markers[x].vsname!=vsName)
					vs.push(GSN.map.markers[x].vsname);
			}
			$.historyLoad(location.hash.substr(1)+",vs="+vs.join(":"));
		}
		/**
		* Update marker
		*/
		,updateMarker: function(vsName,lat,lon){
			var updated = false;
			for (x in GSN.map.markers) {
				var m = GSN.map.markers[x];
				if (m.vsname == vsName) {
					GSN.map.markers[x].setPoint(new GLatLng(lat,lon));	
					updated = true;
					if (GSN.map.highlighted == x)
						GSN.map.trickhighlighted();
				}
			}
			if (!updated)
				GSN.map.addMarker(vsName,lat,lon);
		}
		/**
		* Highlight a marker
		* Stop it if called with null name
		*/
		,followMarker: function(vsName){
			if (!GSN.map.loaded) return;
			
			if (vsName!=null) {
				for (x in GSN.map.markers) {
					var m = GSN.map.markers[x];
					if (m.vsname == vsName) {	
						GSN.map.highlighted = x;
						GSN.map.trickhighlighted();
						GSN.map.autozoomandcenter();
						return;
					}
				}
			}
			
			if (GSN.map.highlighted != null) {
				GSN.map.highlighted = null;	
				map.removeOverlay(GSN.map.highlightedmarker);
			}
		}
		/**
		* Zoom out to see all marker
		*/
		,showAllMarkers: function(){
			var bounds = new GLatLngBounds();
			for (x in GSN.map.markers) {
				if (GSN.map.markerIsVisible(GSN.map.markers[x].vsname))
					bounds.extend(GSN.map.markers[x].getPoint());
			}
			map.setZoom(map.getBoundsZoomLevel(bounds,map.getSize()));
			map.panTo(bounds.getCenter());
		}
	}
	/**
	* Data part
	*/	
	,data : {
	
		fields : new Array(),
		fields_type : new Array(),
		criterias : new Array(),
		nb_crit : 0,
		
		init: function(vsName, radio){
			if (radio == null) {
				radio = false;
			}
   			$("form").attr("action", "");
			$("#dataSet").remove();
			$("#criterias").empty();
			$("#criterias").append("<tr><td class=\"step\">Step 1/5 : Selection of the Virtual Sensor</td></tr>");
			$("#criterias").append("<tr><td class=\"data\" id=\"vsensor\">Selected virtual sensor : " + vsName + "</td></tr>");
			$("#vsensor").append("<input type=\"hidden\" name=\"vsName\" id=\"vsName\" value=\""+vsName+"\">");
			$("#criterias").append("<tr><td class=\"step\">Step 2/5 : Selection of the fields</td></tr>");
			$("#criterias").append("<tr><td class=\"data\" id=\"fields\"></td></tr>");
			$("#fields").append("<input type=\"radio\" id=\"commonReq\" name=\"commonReq\" value=\"true\" onClick=\"javascript:GSN.data.init('" + vsName + "', false)\">Common request ");
			$("#fields").append("<input type=\"radio\" id=\"aggregReq\" name=\"commonReq\" value=\"false\" onClick=\"javascript:GSN.data.init('" + vsName + "', true)\">Aggregate functions<br><br>");
			if (radio) {
				$("#aggregReq").attr("checked", true);
			} else {
				$("#commonReq").attr("checked", true);
			}
				
			$("#fields").append("Select field(s)<br/>");
			$.ajax({
				type: "GET",
				url: "/gsn?REQUEST=113&name="+vsName,
				success: function(msg) {
					GSN.data.fields = new Array();
					GSN.data.fields_type = new Array();
					GSN.data.criterias = new Array();
					GSN.data.nb_crit = 0;
					$("virtual-sensor field", msg).each(function() {
						if ($(this).attr("type").substr(0,3) != "bin") {
							GSN.data.fields.push($(this).attr("name"));
							GSN.data.fields_type.push($(this).attr("type"));
							if (radio) {
								if (($(this).attr("type") == "int") || ($(this).attr("type") == "long") || ($(this).attr("type") == "double")) {
									$("#fields").append("<div id='" + $(this).attr("name") + "'><input type=\"checkbox\" name=\"fields\" id=\"field\" value=\""+$(this).attr("name")+"\" onClick=\"javascript:GSN.data.aggregateSelect('"+$(this).attr("name")+"',this.checked)\">"+$(this).attr("name")+" </div>");
								}
							} else {
								$("#fields").append("<input type=\"checkbox\" name=\"fields\" id=\"field\" value=\""+$(this).attr("name")+"\">"+$(this).attr("name")+"<br/>");
							}
						}
					});
					if (radio) {
						$("#fields").append("<br/>Group by : <select name=\"aggregateGB\" id=\"aggregateGB\" size=\"1\" onChange=\"javascript:GSN.data.groupBy(this.value)\"></select><br/>");
						for (i = 0; i < GSN.data.fields.length; i++) {
							$("#aggregateGB").append("<option value=\"" + GSN.data.fields[i] + "\">" + GSN.data.fields[i] + "</option>");
						}
						$("#aggregateGB").append("<option value=\"none\">None</option>");
					} else {
						$("#fields").append("<br/><input type=\"checkbox\" name=\"all\" onClick=\"javascript:GSN.data.checkAllFields(this.checked)\">Check all<br/>");
					}
					$("#fields").append("<br><a href=\"javascript:GSN.data.nbDatas()\" id=\"nextStep\">Next step</a>");
				}
			});
		},
		aggregateSelect: function(that, checked){
		  // To can choose the aggregate type for the field
		  if (checked) {
    		  $("#"+that).append(" <select name=\""+that+"AG\" id=\""+that+"AG\" size=\"1\"></select>");
    		  $("#"+that+"AG").append("<option value=\"AVG\">AVG</option>");
    		  $("#"+that+"AG").append("<option value=\"MAX\">MAX</option>");
    		  $("#"+that+"AG").append("<option value=\"MIN\">MIN</option>");
    	   } else {
    	       $("#"+that+"AG").remove();
    	   }
						
		},
		groupBy: function(option) {
			if (option == "timed") {
				$("#aggregateGB").after("<input type=\"text\" name=\"gbdelta\" id=\"gbdelta\" size=\"5\"><select name=\"gbdeltameasure\" id=\"gbdeltameasure\" size=\"1\"></select>");
				$("#gbdeltameasure").append("<option value=\"ms\">Milisecond</option>");
				$("#gbdeltameasure").append("<option value=\"s\">Second</option>");
				$("#gbdeltameasure").append("<option value=\"m\">Minute</option>");
				$("#gbdeltameasure").append("<option value=\"h\">Hour</option>");
				$("#gbdeltameasure").append("<option value=\"d\">Day</option>");
			} else {
				$("#gbdelta").remove();
				$("#gbdeltameasure").remove();
			}
		},
		checkAllFields: function(check){
			$("input").each(function () {
				if ($(this).attr("id") == "field") {
					$(this).attr("checked", check);
				}
			});
		},
		nbDatas: function() {
			$("#nextStep").remove();
			$("#criterias").append("<tr><td class=\"step\">Step 3/5 : Selection of the Virtual Sensor</td></tr>");
			$("#criterias").append("<tr><td class=\"data\" id=\"nbDatas\"></td></tr>");
			$("#nbDatas").append("<input type=\"radio\" name=\"nbdatas\" id=\"allDatas\" value=\"\" checked> All data<br/>");
			$("#nbDatas").append("<input type=\"radio\" name=\"nbdatas\" id=\"someDatas\" value=\"\"> Last <input type=\"text\" name=\"nb\" value=\"\" id=\"nbOfDatas\" size=\"4\"/> values<br/>");
			$("#nbDatas").append("<br><a href=\"javascript:GSN.data.addCriteria(true)\" id=\"nextStep\">Next step</a>");
		},	
		addCriteria: function(newStep) {
			if (newStep) {
				$("#nextStep").remove();
				$("#criterias").append("<tr><td class=\"step\">Step 4/5 : Selection of the criterias</td></tr>");
				$("#criterias").append("<tr><td class=\"data\" id=\"where\">");
				$("#where").append("<a id=\"addCrit\" href=\"javascript:GSN.data.addCriteria(false)\">Add criteria</a>");
				$("#where").append("<br/><br/><a id=\"nextStep\" href=\"javascript:GSN.data.selectDataDisplay()\">Next step</a></td></tr>");
			} else {
				GSN.data.nb_crit++;
				newcrit = "<div id=\"where" + GSN.data.nb_crit + "\"></div>";
	    		$("#addCrit").before(newcrit);
	    		GSN.data.addCriteriaLine(GSN.data.nb_crit, "");
	    		GSN.data.criterias.push(GSN.data.nb_crit);
			}
		},
		addCriteriaLine: function(nb_crit, field) {
				newcrit = "";
				if (GSN.data.criterias.length > 0) {
					newcrit += "<select name=\"critJoin\" id=\"critJoin" + nb_crit + "\" size=\"1\">";
					var critJoin = new Array("AND", "OR");
					for (var i=0; i < critJoin.length; i++) {
						newcrit += "<option value=\""+critJoin[i]+"\">"+critJoin[i]+"</option>";
					}
					newcrit += "</select>";
				}
				newcrit += "<select name=\"neg\" size=\"1\" id=\"neg" + nb_crit + "\">";
				var neg = new Array("", "NOT");
	    		for (i=0; i < neg.length; i++) {
	    			newcrit += "<option value=\"" + neg[i] + "\" >" + neg[i] + "</option>";
	    		}
	    		newcrit += "</select> ";
	    		newcrit += "<select name=\"critfield\" id=\"critfield" + nb_crit + "\" size=\"1\" onChange=\"javascript:GSN.data.criteriaForType(this.value,"+nb_crit+")\">";
	    		for (var i=0; i< GSN.data.fields.length; i++) {
	    			newcrit += "<option value=\"" + GSN.data.fields[i] + "\">" + GSN.data.fields[i] + "</option>";
	    		}
	    		newcrit += "</select> ";
	    		var operators = new Array("&gt;", "&ge;", "&lt;", "&le;", "=", "LIKE");
	    		newcrit += "<select name=\"critop\" size=\"1\" id=\"critop" + nb_crit + "\">";
	    		for (i=0; i < operators.length; i++) {
	    			newcrit += "<option value=\"" + operators[i] + "\" >" + operators[i] + "</option>";
	    		}
	    		newcrit += "</select> ";
	    		newcrit += "<input type=\"text\" name=\"critval\" id=\"critval" + nb_crit + "\" size=\"18\">";
	    		newcrit += " <a href=\"javascript:GSN.data.removeCrit("+nb_crit+")\" id=\"remove" + nb_crit + "\"> (remove)</a>";
	    		$("#where"+nb_crit).append(newcrit);
	    		$("#critfield"+nb_crit).attr("value", field);
	    		GSN.data.criteriaForType(GSN.data.fields[0],nb_crit);
		},
		criteriaForType: function(field, nb_crit) {
			if (field == "timed") {
				$("#critval"+nb_crit).val(GSN.util.printDate((new Date()).getTime()));
				$("#critval"+nb_crit).datePicker({startDate:'01/01/2006'});
			} else {
				$("#critval"+nb_crit).val("");
			}
		},
		removeCrit: function(critnb) {
	   		$("#where"+critnb).remove();
	   		var critTMP = new Array();
	   		for (var i=0; i<GSN.data.criterias.length; i++) {
	   			if (GSN.data.criterias[i] == critnb) {
	   				if (i == 0 && GSN.data.criterias.length > 0) {
	   					$("#critJoin"+GSN.data.criterias[i+1]).remove();
	   				}
	   			} else {
	   				critTMP.push(GSN.data.criterias[i]);
	   			}
	   		}
	   		GSN.data.criterias = critTMP;
	   	},
	   	selectDataDisplay: function() {
	   		$("#nextStep").remove();
	   		$("#criterias").append("<tr><td class=\"step\">Step 5/5 : Selection of the format</td></tr>");
			$("#criterias").append("<tr><td class=\"data\" id=\"display\">");
			$("#display").append($.DIV({"id":"showSQL"},$.A({"href":"javascript:GSN.data.getDatas(true);"},"Show SQL query")));
			$("#display").append("<input type=\"radio\" id=\"samePage\" value=\"samepage\" name=\"display\" onClick=\"javascript:GSN.data.showFormatCSV()\" checked>In this page<br/>");
			$("#display").append("<input type=\"radio\" id=\"popup\" value=\"popup\" name=\"display\" onClick=\"javascript:GSN.data.showFormatCSV()\">In a new window<br/>");
			$("#display").append("<input type=\"radio\" id=\"CSV\" value=\"CSV\" name=\"display\" onClick=\"javascript:GSN.data.showFormatCSV()\">Download data<br/>");
			$("#display").append("<br/><a id=\"getDatas\" href=\"javascript:GSN.data.getDatas()\">Get data</a><br/><br/>");
	   	},
	   	showFormatCSV: function() {
	   		if ($("#CSV").attr("checked")) {
	   			$("#getDatas").before($.DIV({"id":"cvsFormat"}));
	   			$("#cvsFormat").append($.INPUT({"type":"radio", "name":"delimiter", "value":"tab"})).append("tab");
	   			$("#cvsFormat").append($.BR()).append($.INPUT({"type":"radio", "name":"delimiter", "value":"space"})).append("space");
	   			$("#cvsFormat").append($.BR()).append($.INPUT({"type":"radio", "name":"delimiter", "value":"other"})).append("other : ");
	   			$("#cvsFormat").append($.INPUT({"type":"text", "name":"otherdelimiter", "size":"2"})).append($.BR()).append($.BR());
	   		} else {
	   			$("#cvsFormat").remove();
	   		}
	   	},
	   	getDatas: function(sql) {
	  		$("table#dataSet","#datachooser").remove();
	   		$("#display").append($.SPAN({"class":"refreshing"},$.IMG({"src":"style/ajax-loader.gif","alt":"loading","title":""})));
	   		if ($("#samePage").attr("checked") || $("#popup").attr("checked") || sql) {
	   			request = "vsName="+$("#vsName").attr("value");
	   			if ($("#commonReq").attr("checked")) {
	   				request += "&commonReq=true";
	   			} else {
	   				request += "&commonReq=false";
	   				if ($("#aggregateGB").val() != "none") {
	   					request += "&groupby=" + $("#aggregateGB").val();
	   					if ($("#aggregateGB").attr("value") == "timed") {
	   						temp = $("#gbdelta").val();
	   						if ($("#gbdeltameasure").val() == "s") {
	   							temp = temp * 1000;
	   						} else if ($("#gbdeltameasure").val() == "m") {
	   							temp = temp * 60000;
	   						} else if ($("#gbdeltameasure").val() == "h") {
	   							temp = temp * 3600000;
	   						} else if ($("#gbdeltameasure").val() == "d") {
	   							temp = temp * 86400000; // 3600000 * 24
	   						}
	   						request += "&groupbytimed=" + temp
	   					}
	   				}
	   			}
	   			$("input").each(function () {
					if ($(this).attr("id") == "field" && $(this).attr("checked")) {
					   if ($("#commonReq").attr("checked")) {
    						request += "&fields=" + $(this).attr("value");
    					} else {
    					   request += "&fields=" + $("#"+$(this).val()+"AG").val()+"("+$(this).attr("value")+")";
    					}
					}
				});
				if ($("#someDatas").attr("checked") && $("#nbOfDatas").attr("value") != "") {
					request += "&nb=" + $("#nbOfDatas").attr("value");
				}
				for (var i=0; i < GSN.data.criterias.length; i++) {
					if (i > 0) {
						request += "&critJoin="+$("#critJoin"+GSN.data.criterias[i]).val();
					}
					request += "&neg="+$("#neg"+GSN.data.criterias[i]).val();
					request += "&critfield="+$("#critfield"+GSN.data.criterias[i]).val();
					request += "&critop="+$("#critop"+GSN.data.criterias[i]).val();
					request += "&critval="+$("#critval"+GSN.data.criterias[i]).val();
				}
				if (sql) {
					request += "&sql=true";
					$.ajax({
						type: "GET",
						url: "/data?"+request,
						success: function(msg) {
							$("#display .refreshing").remove();					
							$("#showSQL .query").remove();
							$("#showSQL").append($.P({"class":"query"},unescape(msg)));
						}
					});
					
				}else
					GSN.data.displayDatas(request);
	   		} /*else if ($("#popup").attr("checked")) {
	   			$("form").attr("target", "_blank");
	   			$("form").attr("action", "/showData.jsp");
	   			document.forms["formular"].submit();
	   		} */ else if ($("#CSV").attr("checked")) {
	   			$("form").attr("action", "/data");
	   			$("#criterias").attr("target", "_self");
	   			document.forms["formular"].submit();
	   			$("#display .refreshing").remove();					
	   		}
	   	},
	   	displayDatas: function(request) {
	   			$.ajax({
				type: "GET",
				url: "/data?"+request,
				success: function(msg) {
					//remove indicator	
					$("#display .refreshing").remove();					
				
	
					//should check no field selected...
					if ($("data", msg).size() == 0) {
						alert(msg);
						return;
					}
					else if ($("line", msg).size() == 0) {
						alert('No data corresponds to your request');
						return;
					}

					var target = "#datachooser";
					if ($("#popup").attr("checked")){
						var w = window.open("", "Data", "width=700,height=700,scrollbars=yes");
						if (w == null) {
							alert('Your browser security setting blocks popup. Please turn it off for this website.');
							return;
						}
						target = w.document.body;
					}

					$("table#dataSet",target).remove();
					$(target).append($.TABLE({"size":"100%", "id":"dataSet"}));
					
					
					var line,tr,rows;
					var lines = $("line", msg);
					for (var i = 0; i<lines.size();i++){
						line = lines.get(i);
						
						if (i==0)
							tr = $.TR({"id":"line"+i, "class":"step"});
						else
							tr = $.TR({"id":"line"+i, "class":"data"});
						
						rows = $("field", line);
						for (var j = 0; j<rows.size();j++){
							$(tr).append($.TD({},$(rows.get(j)).text()));
						}
						$("table#dataSet",target).append(tr);
					}
					
					if (w != null){
						$("table#dataSet .step", target).css("background","#ffa84c")
					}
				}
			});
	   	}
	}
	,util: {
		/**
		* Pretty print of timestamp date
		*/
		printDate: function(date){
			date = new Date(parseInt(date));
			var value = GSN.util.addleadingzero(date.getDate())+"/"+GSN.util.addleadingzero(date.getMonth()+1)+"/"+date.getFullYear();
	        value += " "+GSN.util.addleadingzero(date.getHours())+":"+GSN.util.addleadingzero(date.getMinutes())+":"+GSN.util.addleadingzero(date.getSeconds());	       
	        return value;
	    }
	    /**
		* Add a zero if less then 10
		*/
		,addleadingzero : function (num){
			var n = String(num);
			return (n.length == 1 ? "0"+n : n);
		}
	}	
};