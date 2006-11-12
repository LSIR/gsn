/*
 * gsn javascript
 */
 
var fields = new Array();
var nb_crit = 0;
 
var GSN = { 
	init: function(){
		
	}
	,updateall: function(){
		if ($(".loading",$("#vs")).size()!=0) 
			firstupdate = true;
		else
			firstupdate = false;
		
		if($("#refreshall_timeout").attr("value") > 0 ) {
			setTimeout(GSN.updateall, $("#refreshall_timeout").attr("value" ));
		} else if(!firstupdate) return;
  
		$.ajax({ type: "GET", url: "/gsn", success: function(data){
			if (firstupdate) $(".loading",$("#vs")).remove();
			$("virtual-sensor",data).each(function(){
					if (firstupdate) {
						GSN.vsbox.add($(this).attr("name"));
					}
					GSN.vsbox.update($(this));
						
			});
			if ($("#map").size()>0 && $("#refreshall_autozoomandcenter").attr("checked")){
				//not following any sensor
				if ($("#vs").children().size()==0)
					GSN.map.showAllMarkers();
				else
					GSN.map.centerOnMarker($("#vs").children().get(0).id.substr(6));
			}
		}});
	},
	addandupdate: function(vsName){
		$.ajax({ type: "GET", url: "/gsn?name="+vsName, success: function(data){
			$("virtual-sensor[@name="+vsName+"]",data).each(function(){
					if ($("#vs").children().get(0).id!="vsbox-"+$(this).attr("name")) {
						GSN.vsbox.remove($(this).attr("name"));
						GSN.vsbox.add($(this).attr("name"));
						$("#vs").prepend($("#vsbox-"+$(this).attr("name")));
					}
					GSN.vsbox.update($(this));
					$("#vsbox-"+$(this).attr("name")).animate({ opacity: 'show' },"slow");
			});
		}});
	},
	data: function(vsName){
		$("form").empty();
		$.ajax({
			type: "GET",
			url: "/gsn?REQUEST=113&name="+vsName,
			success: function(msg) {
				fields = new Array();
				$("form").append("<h3 id=\"vname\">" + $("virtual-sensor", msg).attr("name") + "</h3>");
				$("form").append("<p id=\"field\">Fields:</p>");
				$("form").append("<select name=\"fields\" id=\"fields\" multiple size=\"0\"></select>");
				$("field", $("virtual-sensor", msg)).each(function() {
					if ($(this).attr("type").substr(0,3) != "bin") {
						$("select").attr("size", parseInt($("select").attr("size")) + 1);
						$("select").append("<option value=\""+$(this).attr("name")+"\">"+$(this).attr("name")+"</option>");
						fields.push($(this).attr("name"));
					}
				});
				$("form").append("<input type=\"hidden\" name=\"vsName\" value=\""+vsName+"\">");
				$("form").append("<br><br>Number of items : <input type=\"text\" name=\"nb\" size=\"3\">");
				$("form").append("<br><br><a href=\"javascript:GSN.add_criterium()\" id=\"morecrit\">+ Criteria</a>");
				$("form").append("<br><br><input type=\"radio\" name=\"delimiter\" value=\"semicolon\" CHECKED> Semicolon (;)");
				$("form").append("<br><input type=\"radio\" name=\"delimiter\" value=\"tab\"> Tab");
				$("form").append("<br><input type=\"radio\" name=\"delimiter\" value=\"space\"> Space");
				$("form").append("<br><input type=\"radio\" name=\"delimiter\" value=\"other\"> Other : <input type=\"text\" name=\"otherdelimiter\" size=\"3\">");
				$("form").append("<br><br><input type=\"submit\" name=\"submit\" value=\"Get datas\">");
			}
		});
	},
	add_criterium: function() {
		nb_crit++;
		newcrit = "<text id=\"where" + nb_crit + "\"> Where : </text><select name=\"critfield\" id=\"critfield" + nb_crit + "\" size=\"1\">";
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
		newcrit += "<input type=\"text\" name=\"critval\" id=\"critval" + nb_crit + "\" size=\"10\">";
		newcrit += "<a href=\"javascript:GSN.remove_crit("+nb_crit+")\" id=\"remove" + nb_crit + "\"> (remove)</a>";
		newcrit += "<br id=\"br" + nb_crit + "\">";
		$("#morecrit").before(newcrit);
	},
	remove_crit: function(critnb) {
		$("#where"+critnb).remove();
		$("#critfield"+critnb).remove();
		$("#critop"+critnb).remove();
		$("#critval"+critnb).remove();
		$("#remove"+critnb).remove();
		$("#br"+critnb).remove();
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
			//we are in the normal context
			//$("#vs").empty();
			GSN.addandupdate(vsName);

		}
	},
	debug: function (txt) {
		if(typeof console != "undefined" && false) {
			console.debug(txt);
		}	
	},
	//box showing all vs info
	vsbox: {
		container: "#vs"
		,add: function(vsName) {
			//create the vs box if it doesn't exist
			var vsdiv = "vsbox-"+vsName;
			if ($("#"+vsdiv, $(this.container)).size()==0) {
				GSN.debug("add:"+vsdiv);
				$(this.container).append($.DIV({"id":vsdiv,"class":"vsbox"},
									  $.H3({},$.SPAN({},vsName),
									  	//$.SPAN({"class":"id"},"0"),
									  	//$.SPAN({"class":"status"},"live")),
									  	$.A({"href":"javascript:GSN.vsbox.remove('"+vsName+"');"},"close"),
								      	$.SPAN({"class":"timed"},"")
									  	//$.A({"class":"freeze","href":"javascript:GSN.freezevs('"+vsdiv+"');"},"freeze")
									    ),
									  $.DL({"class":"dynamic"}),
									  $.P({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','static');"},"addressing")),
									  $.DL({"class":"static"}),
									  $.P({},$.A({"href":"javascript:GSN.vsbox.toggle('"+vsName+"','structure');"},"structure")),
									  $.DL({"class":"structure"})
									  ));
				$("#"+vsdiv+" > dl.static", $(this.container)).hide();
				$("#"+vsdiv+" > dl.structure", $(this.container)).hide();
				
			}
		}
		,update: function (vs){

			var vsdiv = "vsbox-"+vs.attr("name");
						//var id = $("#"+vsid+" span.id", where);
			//var status = $("#"+vsid+" span.status", where);
			GSN.debug("U:"+vsdiv);			
			var dl = $("#"+vsdiv+" > dl.dynamic ", $(this.container));
			if (dl.size()!=0){
			  $("field",vs).each(function(){ 
				var name = $(this).attr("name");
				var type = $(this).attr("type");
				var value = $(this).text();
				
				if (type=="predicate")
					dl = $("#"+vsdiv+" > dl.static ", $(this.container));
				else {
					//add to structure
					var struct = $("#"+vsdiv+" > dl.structure ", $(this.container));
					if ($("."+name, struct).size()==0){
						//create name:value
						struct.append($.DT({},name));
						struct.append($.DD({"class":name},type));
					}
				}
					
				if (name=="timed") return;
			
				//create the dt/dd line if it doesn't exist
				if ($("."+name, dl).size()==0){
					//create name:value
					dl.append($.DT({},name));
					dl.append($.DD({"class":name}));
				} 
				//update the value
				if (value == "") {
					$("."+name, dl).empty().append("null");
				} else if ($(this).attr("type").indexOf("svg") != -1){
					if ($("embed",$("."+name, dl)).size()==0)
						$("."+name, dl).empty().append('<embed type="image/svg+xml" width="400" height="400" src="'+value+'" PLUGINSPAGE="http://www.adobe.com/svg/viewer/install/" />');
					else
						$("embed",$("."+name, dl)).attr("src",value);
				} else if ($(this).attr("type").indexOf("image") != -1){
					if ($("img",$("."+name, dl)).size()==0)
						$("."+name, dl).empty().append('<img src="'+value+'" alt="error" />');
					else
						$("img",$("."+name, dl)).attr("src",value);
				} else if ($(this).attr("type").indexOf("binary") != -1){
					if ($("a",$("."+name, dl)).size()==0)
						$("."+name, dl).empty().append('<a href="'+value+'">download <img src="style/download_arrow.gif" alt="" /></a>');
					else
						$("a",$("."+name, dl)).attr("href",value);
				} else {
					if ($("."+name, dl).text() != value) {
						GSN.debug("Update: "+$("."+name, dl).text()+"->"+value);
						$("."+name, dl).empty().append(value);
					}
				}
			  });
			
			
			  var value = $("field[@name=timed]",vs).text();	
			  if (value != "") {
			  	var date = new Date(parseInt(value));
			  	value = date.getFullYear()+"/"+addleadingzero(date.getMonth()+1)+"/"+addleadingzero(date.getDate());
	          	value += "@"+addleadingzero(date.getHours())+":"+addleadingzero(date.getMinutes())+":"+addleadingzero(date.getSeconds());
	    	  	if ($("#"+vsdiv+" span.timed", $(this.container)).text() != value) {
					$("#"+vsdiv+" span.timed", $(this.container)).empty().append(value);
			  	}
			  }
			}
			
			//when map is enable
			if ($("#map").size()>0){
				var lat = $("field[@name=latitude]",vs).text();
				var lon = $("field[@name=longitude]",vs).text();
				if (lat != "" && lon != ""){
					GSN.map.updateMarker(vs.attr("name"),lat,lon);
				} /*else
					GSN.map.showAllMarkers();*/
			}
		}
		,remove: function (vsName) {
			var vsdiv = "vsbox-"+vsName;
			$("#"+vsdiv).remove();
			//$("#"+vsdiv).id("#"+vsdiv+"-remove").animate({ opacity: 'hide' }, "slow", function(){ console.warn("remove: "+"#"+vsdiv+"-remove");$("#"+vsdiv+"-remove").remove(); });
			
		}
		,toggle: function (vsName,dl){
			var vsdiv = "vsbox-"+vsName;
			$("#"+vsdiv+" > dl."+dl, $(this.container)).toggle();
		}
	},
	map: {
		markers : new Array()
		,addMarker: function(vsName,lat,lon){
			if (!map.isLoaded())
				map.setCenter(new GLatLng(lat,lon), 13);
		
			var marker = new GMarker(new GLatLng(lat,lon));
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
};
 
function addleadingzero(num){
	var n = String(num);
	return (n.length == 1 ? "0"+n : n);
}