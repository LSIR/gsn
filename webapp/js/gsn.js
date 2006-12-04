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
	/**
	* Initialize a page load (begin, tab click & back button)
	*/
	,load: function(){
		GSN.debug("init:"+location.hash);
		var params=location.hash.substr(1).split(",");
		
		GSN.context = params[0];
		//highlight the right tab in the navigation bar
		$("#navigation li").each(function(){
			if($("a",$(this)).text()==GSN.context)
				$(this).addClass("selected");
			else
				$(this).removeClass("selected");
		});
		
		$("#main > div").hide();
		//for each page context
		if (GSN.context=="home")	{
			GSN.vsbox.container = "#vs";
			$("#main #control").show();
			$("#main #homediv").show();
			$("#control #closeall").show();
			//load and display all the visual sensors
			if (!GSN.loaded) GSN.updateall();
		} else if (GSN.context=="data")	{
			$(".intro").remove();
			$("#main #datachooser").show();
			if (!GSN.loaded) GSN.updateall();
		} else if (GSN.context=="map")	{
			GSN.vsbox.container = "#vs4map";
			$(".intro").remove();
			$("#main #control").show();
			$("#control #closeall").hide();
			$("#main #mapdiv").show();
			if(!GSN.map.loaded) {
				GSN.map.init();
				GSN.updateall();
			}
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
			$("#vs4map").empty();
			GSN.map.followMarker(vsName);
			GSN.addandupdate(vsName);
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
		if ($(document).title()=="GSN") {
			var gsn = $("gsn",data);
			$(document).title($(gsn).attr("name")+" :: GSN");
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
				GSN.vsbox.update($(this));
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
					GSN.vsbox.update($(this));
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
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','structure');","class":"tabstructure"},"structure"))
									      ),
									  $.DL({"class":"dynamic"}),
									  $.DL({"class":"static"}),
									  $.DL({"class":"structure"})
			));
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
					GSN.map.updateMarker(vs.attr("name"),lat,lon);
				}
			}
			
			//update the vsbox
			var vsd = $(".vsbox-"+vs.attr("name"), $(this.container));
			if (vsd.size()==0) return;
			//if (vsd.css("display")=="none") return;
			
			var vsdl = $("dl", vsd);
			var dynamic = vsdl.get(0);
			var static = vsdl.get(1);
			var struct = vsdl.get(2);
			dl = dynamic;
	
			var name,type,value;
			//update the vsbox the first time, when it's empty
			if ($(dynamic).children().size()==0 && $(static).children().size()==0){
			  $("field",vs).each(function(){ 
				name = $(this).attr("name");
				type = $(this).attr("type");
				value = $(this).text();
				
				if (name=="timed") {
			  		if (value != "") value = GSN.util.printDate(value);
					$("span.timed", vsd).empty().append(value);
			  		return;
			  	}
				
				if (type=="predicate")
					dl = static;
				else //add to structure
					$(struct).append($.DT({},name),$.DD({"class":name},type));
							
				//set the value
				if (value == "") {
					value = "null";
				} else if (type.indexOf("svg") != -1){
					value = '<embed type="image/svg+xml" width="400" height="400" src="'+value+'" PLUGINSPAGE="http://www.adobe.com/svg/viewer/install/" />';
				} else if (type.indexOf("image") != -1){
					value = '<img src="'+value+'" alt="error" />';
				} else if (type.indexOf("binary") != -1){
					value = '<a href="'+value+'">download <img src="style/download_arrow.gif" alt="" /></a>';
				} 
				$(dl).append('<dt>'+name+'</dt><dd class="'+name+'">'+value+'</dd>');				
			  });
			  return true;
			} else {
				//update the vsbox when the value already exists
				var dds = $("dd",dl);
				var dd,field;
				for (var i = 0; i<dds.size();i++){
					dd = dds.get(i);
					field = $("field[@name="+$(dd).attr("class")+"]",vs)
					type = $(field).attr("type");
					value = $(field).text();
					if (value!="") {
						if (type.indexOf("svg") != -1){
							$("embed",$(dd)).attr("src",value);
						} else if (type.indexOf("image") != -1){
							$("img",$(dd)).attr("src",value);
						} else if (type.indexOf("binary") != -1){
							$("a",$(dd)).attr("href",value);
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
					if(overlay)	//when a marker is clicked
						if(typeof overlay.vsname != "undefined") 
							GSN.menu(overlay.vsname);
				});		
				GEvent.addListener(map, 'zoomend', function (oldzoomlevel,newzoomlevel) {
  					GSN.map.zoomend(oldzoomlevel,newzoomlevel);
				}); 		
   			}
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
  			
  			if(GSN.context=="fullmap"){
				var vs = $(".vsbox-"+vsName+" > h3 > span.vsname")
				$(vs).wrap("<a href=\"javascript:GSN.menu('"+$(vs).text()+"');\"></a>");
			}
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
				bounds.extend(GSN.map.markers[x].getPoint());
			}
			map.setZoom(map.getBoundsZoomLevel(bounds,map.getSize()));
			map.setCenter(bounds.getCenter());
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
					fields = new Array();
					fields_type = new Array();
					criterias = new Array();
					nb_crit = 0;
					$("field", $("virtual-sensor", msg)).each(function() {
						if ($(this).attr("type").substr(0,3) != "bin") {
							fields.push($(this).attr("name"));
							fields_type.push($(this).attr("type"));
							if (radio) {
								if (($(this).attr("type") == "int") || ($(this).attr("type") == "long") || ($(this).attr("type") == "double")) {
									$("#fields").append("<input type=\"radio\" name=\"fields\" id=\"field\" value=\""+$(this).attr("name")+"\">"+$(this).attr("name")+"<br/>");
								}
							} else {
								$("#fields").append("<input type=\"checkbox\" name=\"fields\" id=\"field\" value=\""+$(this).attr("name")+"\">"+$(this).attr("name")+"<br/>");
							}
						}
					});
					if (radio) {
						$("#field").attr("checked", true);
						$("#fields").append("<br/><select name=\"aggregateFunction\" id=\"aggregateFunction\" size=\"1\"></select><br/>");
						$("#aggregateFunction").append("<option value=\"AVG\">AVG</option>");
						$("#aggregateFunction").append("<option value=\"AVG\">MAX</option>");
						$("#aggregateFunction").append("<option value=\"AVG\">MIN</option>");
					} else {
						$("#fields").append("<br/><input type=\"checkbox\" name=\"all\" onClick=\"javascript:GSN.data.checkAllFields(this.checked)\">Check all<br/>");
					}
					$("#fields").append("<br><a href=\"javascript:GSN.data.nbDatas()\" id=\"nextStep\">Next step</a>");
				}
			});
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
			$("#nbDatas").append("<input type=\"radio\" name=\"nbdatas\" id=\"allDatas\" value=\"\" checked> All datas<br/>");
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
				nb_crit++;
				newcrit = "<div id=\"where" + nb_crit + "\"></div>";
	    		$("#addCrit").before(newcrit);
	    		GSN.data.addCriteriaLine(nb_crit, "");
	    		criterias.push(nb_crit);
			}
		},
		addCriteriaLine: function(nb_crit, field) {
				newcrit = "";
				if (criterias.length > 0) {
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
	    		newcrit += "<select name=\"critfield\" id=\"critfield" + nb_crit + "\" size=\"1\" onChange=\"javascript:GSN.criteriaForType(this.value,"+nb_crit+")\">";
	    		for (var i=0; i< fields.length; i++) {
	    			newcrit += "<option value=\"" + fields[i] + "\">" + fields[i] + "</option>";
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
		},
		criteriaForType: function(field, nb_crit) {
			if (field == "TIMED") {
				$("#critval"+nb_crit).val("DD/MM/YYYY hh:mm:ss");
			} else {
				$("#critval"+nb_crit).val("");
			}
		},
		removeCrit: function(critnb) {
	   		$("#where"+critnb).remove();
	   		var critTMP = new Array();
	   		for (var i=0; i<criterias.length; i++) {
	   			if (criterias[i] == critnb) {
	   				if (i == 0 && criterias.length > 0) {
	   					$("#critJoin"+criterias[i+1]).remove();
	   				}
	   			} else {
	   				critTMP.push(criterias[i]);
	   			}
	   		}
	   		criterias = critTMP;
	   	},
	   	selectDataDisplay: function() {
	   		$("#nextStep").remove();
	   		$("#criterias").append("<tr><td class=\"step\">Step 5/5 : Selection of the format</td></tr>");
			$("#criterias").append("<tr><td class=\"data\" id=\"display\">");
			$("#display").append("<input type=\"radio\" id=\"samePage\" value=\"samepage\" name=\"display\" onClick=\"javascript:GSN.data.showFormatCSV()\" checked>In this page<br/>");
			$("#display").append("<input type=\"radio\" id=\"popup\" value=\"popup\" name=\"display\" onClick=\"javascript:GSN.data.showFormatCSV()\">In a new window<br/>");
			$("#display").append("<input type=\"radio\" id=\"CSV\" value=\"CSV\" name=\"display\" onClick=\"javascript:GSN.data.showFormatCSV()\">Download datas<br/>");
			$("#display").append("<br/><a id=\"getDatas\" href=\"javascript:GSN.data.getDatas()\">Get datas</a><br/><br/>");
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
	   	getDatas: function() {
	   		if ($("#samePage").attr("checked")) {
	   			request = "vsName="+$("#vsName").attr("value");
	   			if ($("#commonReq").attr("checked")) {
	   				request += "&commonReq=true";
	   			} else {
	   				request += "&commonReq=false";
	   				request += "&aggregateFunction=" + $("#aggregateFunction").val();
	   			}
	   			$("input").each(function () {
					if ($(this).attr("id") == "field" && $(this).attr("checked")) {
						request += "&fields=" + $(this).attr("value");
					}
				});
				if ($("#someDatas").attr("checked") && $("#nbOfDatas").attr("value") != "") {
					request += "&nb=" + $("#nbOfDatas").attr("value");
				}
				for (var i=0; i < criterias.length; i++) {
					if (i > 0) {
						request += "&critJoin="+$("#critJoin"+criterias[i]).val();
					}
					request += "&neg="+$("#neg"+criterias[i]).val();
					request += "&critfield="+$("#critfield"+criterias[i]).val();
					request += "&critop="+$("#critop"+criterias[i]).val();
					request += "&critval="+$("#critval"+criterias[i]).val();
				}
				GSN.data.displayDatas(request);
	   		} else if ($("#popup").attr("checked")) {
	   			$("form").attr("target", "_blank");
	   			$("form").attr("action", "/showData.jsp");
	   			document.forms["formular"].submit();
	   		} else if ($("#CSV").attr("checked")) {
	   			$("form").attr("action", "/data");
	   			$("#criterias").attr("target", "_self");
	   			document.forms["formular"].submit();
	   		}
	   	},
	   	displayDatas: function(request) {
	   			$.ajax({
				type: "GET",
				url: "/data?"+request,
				success: function(msg) {
					$("#dataSet").remove();
					$("#datachooser").append($.TABLE({"size":"100%", "id":"dataSet"})); //"<table size=\"100%\" id=\"dataSet\">");
					nbLine = 0;
					$("line", $("data", msg)).each(function() {
						nbLine++;
						if (nbLine == 1) {
							$("#dataSet").append($.TR({"id":"line"+nbLine, "class":"step"}));
						} else {
							$("#dataSet").append($.TR({"id":"line"+nbLine, "class":"data"}));
						}
						$("field", $(this)).each(function() {
							$("#line"+nbLine).append($.TD({},$(this).text()));
						});
						
					});
					if (nbLine == 0) {
						alert('No data corresponds to your request');
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
			var value = date.getFullYear()+"/"+GSN.util.addleadingzero(date.getMonth()+1)+"/"+GSN.util.addleadingzero(date.getDate());
	        value += "@"+GSN.util.addleadingzero(date.getHours())+":"+GSN.util.addleadingzero(date.getMinutes())+":"+GSN.util.addleadingzero(date.getSeconds());	       
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