/*
 * gsn javascript
 */
var map;
var tinyred;
var tinygreen;

var fields = new Array();
var fields_type = new Array();
var criterias = new Array();
var nb_crit = 0;
 
 
var GSN = { 
	debugmode: true
	,init: function(){
		GSN.debug("init:"+location.hash);
		
		var params=location.hash.substr(1).split(",");
		$("#navigation li").each(function(){
			if($("a",$(this)).text()==params[0])
				$(this).addClass("selected");
			else
				$(this).removeClass("selected");
		});
		
		$("#main > div").hide();
		if (params[0]=="home")	{
			$("#main .intro").show();
			
			$("#main #homediv").show();
			
			//load and display all the visual sensors
			GSN.updateall();
		} else if (params[0]=="data")	{
			$("#main #datachooser").show();
		} else if (params[0]=="map")	{
			$("#main #homediv").show();
			$("#main #mapdiv").show();
			if(!GSN.map.loaded)
				GSN.map.init();
			GSN.updateall();
		}
	}
	,nav: function (page) {
		location.hash=page;
		GSN.init();
		return false;
	}
	,debug: function (txt) {
		if(typeof console != "undefined" && this.debugmode) {
			console.debug(txt);
		}	
	}
	,updatenb: 0
	,updateallchange: function(){
		if($("#refreshall_timeout").attr("value") != 0)
			GSN.updateall();
	}
	,updateall: function(num){
		//to prevent multiple update instance
		if (typeof num == "number" && num != GSN.updatenb) return;
		GSN.updatenb++;
		if ($(".loading",$("#vs")).size()!=0) 
			firstupdate = true;
		else
			firstupdate = false;
  
  		$(".refreshing").show();
  		
		$.ajax({ type: "GET", url: "/gsn", success: function(data){
			if (firstupdate) $(".loading",$("#vs")).remove();
			var addedvs = 0;
			
			
			if ($(document).title()=="GSN") {
				var gsn = $("gsn",data);
				$(document).title($(gsn).attr("name")+" :: GSN");
				$("#gsn-name").empty().append($(gsn).attr("name")+" :: GSN");
				$("#gsn-desc").empty().append($(gsn).attr("description"));
				$("#gsn-author").empty().append($(gsn).attr("author")+" ("+$(gsn).attr("email")+")");
			}
			$("#vsmenu").empty();
			var start = new Date();
			var vsname;
			$("virtual-sensor",data).each(function(){
				vsname = $(this).attr("name");
				$("#vsmenu").append($.LI({},$.A({"href":"javascript:GSN.menu('"+vsname+"');","id":"menu-"+vsname+""},vsname)));
			
				if (firstupdate /*&& addedvs < 5*/) {
					addedvs++;
					GSN.vsbox.add(vsname);
				}
				GSN.vsbox.update($(this))
				if (firstupdate)
					$("#vsbox-"+vsname).fadeIn("slow");
						
			});
			var diff = new Date() - start;
			GSN.debug("updateall time:"+diff/1000); 
			if (GSN.map.loaded && $("#refreshall_autozoomandcenter").attr("checked")){
				//not following any sensor
				if ($("#vs").children().size()==1)
					GSN.map.centerOnMarker($("#vs").children().get(0).id.substr(6));
				else
					GSN.map.showAllMarkers();
			}
			
			if($("#refreshall_timeout").attr("value") > 0)
				setTimeout("GSN.updateall("+GSN.updatenb+")", $("#refreshall_timeout").attr("value"));
			$(".refreshing").hide();	
		}});
	},
	addandupdate: function(vsName){
		if ($("#vsbox-"+vsName).size()!=0)
			$("#vsbox-"+vsName).hide();
		else
			GSN.vsbox.add(vsName);
		
		$("#vs").prepend($("#vsbox-"+vsName));
		$("#vsbox-"+vsName).fadeIn("slow");
				
		/*if ($("#vs").children().get(0).id!="vsbox-"+vsName) {
			//GSN.vsbox.remove(vsName);
			//GSN.vsbox.add(vsName);
			
		}*/
		$.ajax({ type: "GET", url: "/gsn?name="+vsName, success: function(data){
			$("virtual-sensor[@name="+vsName+"]",data).each(function(){
					GSN.vsbox.update($(this));
					GSN.map.centerOnMarker($(this).attr("name"));
			});
		}});
	},
	data: function(vsName){
		$("#dataSet").remove();
		$("#criterias").empty();
		$("#criterias").append("<tr><td class=\"step\">Step 1/5 : Selection of the Virtual Sensor</td></tr>");
		$("#criterias").append("<tr><td class=\"data\" id=\"vsensor\">Selected virtual sensor : " + vsName + "</td></tr>");
		$("#vsensor").append("<input type=\"hidden\" name=\"vsName\" id=\"vsName\" value=\""+vsName+"\">");
		$("#criterias").append("<tr><td class=\"step\">Step 2/5 : Selection of the fields</td></tr>");
		$("#criterias").append("<tr><td class=\"data\" id=\"fields\">Select fields<br/></td></tr>");
		$.ajax({
			type: "GET",
			url: "/gsn?REQUEST=113&name="+vsName,
			success: function(msg) {
				fields = new Array();
				criterias = new Array();
				$("field", $("virtual-sensor", msg)).each(function() {
					if ($(this).attr("type").substr(0,3) != "bin") {
						fields.push($(this).attr("name"));
						fields_type.push($(this).attr("type"));
						$("#fields").append("<input type=\"checkbox\" name=\"fields\" id=\"field\" value=\""+$(this).attr("name")+"\">"+$(this).attr("name")+"<br/>");
					}
				});
				$("#fields").append("<br/><input type=\"checkbox\" name=\"all\" onClick=\"javascript:GSN.checkAllFields(this.checked)\">Check all<br/>");
				$("#fields").append("<br><a href=\"javascript:GSN.nbDatas()\" id=\"nextStep\">Next step</a>");
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
		$("#nbDatas").append("<br><a href=\"javascript:GSN.addCriteria(true)\" id=\"nextStep\">Next step</a>");
	},	
	addCriteria: function(newStep) {
		if (newStep) {
			$("#nextStep").remove();
			$("#criterias").append("<tr><td class=\"step\">Step 4/5 : Selection of the criterias</td></tr>");
			$("#criterias").append("<tr><td class=\"data\" id=\"where\">");
			$("#where").append("<a id=\"addCrit\" href=\"javascript:GSN.addCriteria(false)\">Add criteria</a>");
			$("#where").append("<br/><br/><a id=\"nextStep\" href=\"javascript:GSN.selectDataDisplay()\">Next step</a></td></tr>");
		} else {
			nb_crit++;
			newcrit = "<div id=\"where" + nb_crit + "\"></div>";
    		$("#addCrit").before(newcrit);
    		GSN.addCriteriaLine(nb_crit, "");
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
    		newcrit += " <a href=\"javascript:GSN.removeCrit("+nb_crit+")\" id=\"remove" + nb_crit + "\"> (remove)</a>";
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
		$("#display").append("<input type=\"radio\" id=\"samePage\" value=\"samepage\" name=\"display\" checked>In this page<br/>");
		//$("#display").append("<input type=\"radio\" id=\"popup\" value=\"popup\" name=\"display\">In a new window<br/>");
		$("#display").append("<input type=\"radio\" id=\"CSV\" value=\"CSV\" name=\"display\">Download datas<br/>");
		$("#display").append("<br/><a href=\"javascript:GSN.getDatas()\">Get datas</a><br/><br/>");
   	},
   	getDatas: function() {
   		if ($("#samePage").attr("checked")) {
   			request = "vsName="+$("#vsName").attr("value");;
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
			}
		});
		/*
   		} else if ($("#popup").attr("checked")) {
   			$("form").attr("target", "_blank");
   			document.forms["formular"].submit();
   		*/
   		} else if ($("#CSV").attr("checked")) {
   			$("#criterias").attr("target", "_self");
   			document.forms["formular"].submit();
   		}
   	},
	menu: function (vsName) {
		GSN.debug("menu:"+vsName);
		
		if ($("#map").size()>0){
			//we are in the map context
			if ($("#vs").children().get(0).id != "vsbox-"+vsName) 
				$("#vs").empty();
			GSN.addandupdate(vsName);
			GSN.map.centerOnMarker(vsName);
		} else {
			$(".intro").remove();
			//we are in the normal context
			//$("#vs").empty();
			GSN.addandupdate(vsName);

		}
	}
	,closeall: function (){
		$("#vs").empty();
	}
	//box showing all vs info
	,vsbox: {
		container: "#vs"
		,add: function(vsName) {
			//create the vs box if it doesn't exist
			var vsdiv = "vsbox-"+vsName;
			if ($("#"+vsdiv, $(this.container)).size()==0) {
				//GSN.debug("add:"+vsdiv);
				$(this.container).append($.DIV({"id":vsdiv,"class":"vsbox"},
									  $.H3({},$.SPAN({},vsName),
									  	//$.SPAN	({"class":"id"},"0"),
									  	//$.SPAN({"class":"status"},"live")),
									  	$.A({"href":"javascript:GSN.vsbox.remove('"+vsName+"');"},"close"),
								      	$.SPAN({"class":"timed"},"loading...")
									  	//$.A({"class":"freeze","href":"javascript:GSN.freezevs('"+vsdiv+"');"},"freeze")
									    ),$.UL({"class":"tabnav"},
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','dynamic');","class":"tabdynamic"},"dynamic")),
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','static');","class":"tabstatic"},"addressing")),
									    	$.LI({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','structure');","class":"tabstructure"},"structure"))
									      ),
									  $.DL({"class":"dynamic"}),
									  //$.P({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','static');"},"addressing")),
									  $.DL({"class":"static"}),
									  //$.P({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','structure');"},"structure")),
									  $.DL({"class":"structure"})
									  ));
				//$("#"+vsdiv+" > dl.static", $(this.container)).hide();
				//$("#"+vsdiv+" > dl.structure", $(this.container)).hide();
				
			}
			$("#"+vsdiv).hide();
			GSN.vsbox.toggle(vsName,'dynamic');
		}
		,update: function (vs){
			//when map is enable
			if (GSN.map.loaded){
				var lat = $("field[@name=latitude]",vs).text();
				var lon = $("field[@name=longitude]",vs).text();
				if (lat != "" && lon != ""){
					GSN.map.updateMarker(vs.attr("name"),lat,lon);
				}
			}
			
			//update the vsbox
			var vsd = $("#vsbox-"+vs.attr("name"), $(this.container));
			if (vsd.size()==0) return;
			
			var vsdl = $("dl", vsd);
			var dynamic = vsdl.get(0);
			var static = vsdl.get(1);
			var struct = vsdl.get(2);
			dl = dynamic;
	
			var name,type,value;
	
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
		,remove: function (vsName) {
			GSN.debug("remove: "+vsName);
			var vsdiv = "vsbox-"+vsName;
			$("#"+vsdiv).remove();
			//$("#"+vsdiv).id("#"+vsdiv+"-remove").animate({ opacity: 'hide' }, "slow", function(){ console.warn("remove: "+"#"+vsdiv+"-remove");$("#"+vsdiv+"-remove").remove(); });
			if ($("#map").size()>0){
				if (GSN.map.highlighted != null) {
					GSN.map.highlighted = null;	
					map.removeOverlay(GSN.map.highlightedmarker);
				}
			}
		}
		,toggle: function (vsName,dl){
			var vsdiv = "vsbox-"+vsName;
			$("#"+vsdiv+" > dl", $(this.container)).hide();
			$("#"+vsdiv+" > dl."+dl, $(this.container)).show();
			$("#"+vsdiv+" a", $(this.container)).removeClass("active");
			$("#"+vsdiv+" a.tab"+dl, $(this.container)).addClass("active");
		}
	},
	map: {
		loaded: false
		,init : function(){
			this.loaded=true;
		
		if(typeof GBrowserIsCompatible == "undefined") {
		$("#map").append($.P({"class":"error"},"Google maps isn't loaded! Maybe your internet connection is not working."));
	} else if (GBrowserIsCompatible()) {
		GSN.debug("init gmap");
       
        map = new GMap2(document.getElementById("map"));
        //some fun
        map.addControl(new GLargeMapControl());
		map.addControl(new GMapTypeControl());
		map.addControl(new GScaleControl());
		map.addControl(new GOverviewMapControl());

/*

				// custom map
				//-----------
				// copyright
				var copyright = new GCopyright(1, new GLatLngBounds(new GLatLng(-90, -180), new GLatLng(90, 180)), 16, "©2006 EPFL");
				// copyright collection
				var copyrightCollection = new GCopyrightCollection('Imagery');
				copyrightCollection.addCopyright(copyright);
				// retrieve the tiles location
				customGetTileUrl = function(a, b) {
					return "http://sensorscope.epfl.ch/map/image/" + a.x + "_" + a.y + "_" + (17 - b) + ".jpg"
				}
				// tile layers
				var tileLayers = [new GTileLayer(copyrightCollection, 16, 17)];
				tileLayers[0].getTileUrl = customGetTileUrl;
				// display the custom map
				var customMap = new GMapType(tileLayers, new GMercatorProjection(18), "Aerial", {errorMessage:"Aerial imagery unavailable."});
				map.addMapType(customMap);
	*/			


		// Create our "tiny" marker icon
		tinyred = new GIcon();
		tinyred.image = "http://labs.google.com/ridefinder/images/mm_20_red.png";
		tinyred.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
		tinyred.iconSize = new GSize(12, 20);
		tinyred.shadowSize = new GSize(22, 20);
		tinyred.iconAnchor = new GPoint(6, 20);
		tinyred.infoWindowAnchor = new GPoint(5, 1);
		
		// Create our "tiny" marker icon
		tinygreen = new GIcon();
		tinygreen.image = "http://labs.google.com/ridefinder/images/mm_20_green.png";
		tinygreen.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
		tinygreen.iconSize = new GSize(12, 20);
		tinygreen.shadowSize = new GSize(22, 20);
		tinygreen.iconAnchor = new GPoint(6, 40);
		tinygreen.infoWindowAnchor = new GPoint(5, 1);

		
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
		,markers : new Array()
		,highlighted : null
		,highlightedmarker : null
		,zoomend : function(oldzoomlevel,newzoomlevel){
			GSN.map.trickhighlighted();
		}
		,trickhighlighted : function(){
			if (GSN.map.highlighted != null) {
				var hPoint = map.getCurrentMapType().getProjection().fromLatLngToPixel(GSN.map.markers[GSN.map.highlighted].getPoint(),map.getZoom());
    			var marker = new GMarker(map.getCurrentMapType().getProjection().fromPixelToLatLng(new GPoint(hPoint.x , hPoint.y + 20 ) , map.getZoom()),tinygreen);
    			map.removeOverlay(GSN.map.highlightedmarker);
    			GSN.map.highlightedmarker = marker;
  				map.addOverlay(marker);
      		}
		}
		,addMarker: function(vsName,lat,lon){
			if (!map.isLoaded())
				map.setCenter(new GLatLng(lat,lon), 13);
		
			var marker = new GMarker(new GLatLng(lat,lon),tinyred);
  			marker.vsname = vsName;
  			GSN.map.markers.push(marker);
  			map.addOverlay(marker);
  					
  			//add gpsenable class
  			$("#menu-"+vsName).addClass("gpsenabled");
		}
		,updateMarker: function(vsName,lat,lon){
			var updated = false;
			for (x in GSN.map.markers) {
				var m = GSN.map.markers[x];
				if (m.vsname == vsName) {
					GSN.map.markers[x].setPoint(new GLatLng(lat,lon));	
					updated = true;
				}
			}
			if (!updated)
				GSN.map.addMarker(vsName,lat,lon);
		}
		,centerOnMarker: function(vsName){
			for (x in GSN.map.markers) {
				var m = GSN.map.markers[x];
				if (m.vsname == vsName) {		
					map.panTo(GSN.map.markers[x].getPoint());	
					//GSN.map.markers[x].openInfoWindow(m.vsname);
					
					GSN.map.highlighted=x;
					GSN.map.trickhighlighted();
  		
					//map.removeOverlay(GSN.map.markers[x]);
					//GSN.map.markers.splice(x,1);
					
				}
			}
		},showAllMarkers: function(){
			var bounds = new GLatLngBounds();
			for (x in GSN.map.markers) {
				bounds.extend(GSN.map.markers[x].getPoint());
			}
			map.setZoom(map.getBoundsZoomLevel(bounds,map.getSize()));
			map.setCenter(bounds.getCenter());
		}
	} 
	/*,changevs: function (vsid,df) {
		var id = $("#"+vsid+" > h3 > span > span.id");	
		newid = parseInt(id.text()) + df;
		id.empty().append(newid);
		//console.debug("nothing: "+newid);
	},
	freezevs: function (vsdiv) {
		var status = $("#"+vsdiv+" span.status");
		
		
		var freeze = $("#"+vsdiv+" a.freeze");
		if (freeze.text()=="freeze"){
			status.empty().append("paused");
			freeze.empty().append("unfreeze");
			//freeze.after($.A({class:"nv",href:"javascript:GSN.changevs('"+vsdiv+"',-1);"},"prev"));
			//freeze.before($.A({class:"nv",href:"javascript:GSN.changevs('"+vsdiv+"',+1);"},"next"));
		} else {
			status.empty().append("live");
			freeze.empty().append("freeze");
			//freeze.siblings(".nv").remove();
		}
	}*/
	,util: {
		printDate: function(date){
			date = new Date(parseInt(date));
			var value = date.getFullYear()+"/"+GSN.util.addleadingzero(date.getMonth()+1)+"/"+GSN.util.addleadingzero(date.getDate());
	        value += "@"+GSN.util.addleadingzero(date.getHours())+":"+GSN.util.addleadingzero(date.getMinutes())+":"+GSN.util.addleadingzero(date.getSeconds());	       
	        return value;
	    }
		,addleadingzero : function (num){
			var n = String(num);
			return (n.length == 1 ? "0"+n : n);
		}
	}	
};
 
