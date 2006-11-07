/*
 * gsn javascript
 */
 
var GSN = { 
	updateall: function(){
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
					if (firstupdate) 
						GSN.vsbox.add($(this).attr("name"));
					GSN.vsbox.update($(this));
			});
		}});
	},
	addandupdate: function(vsName){
		$.ajax({ type: "GET", url: "/gsn?name="+vsName, success: function(data){
			$("virtual-sensor[@name="+vsName+"]",data).each(function(){
					GSN.vsbox.add($(this).attr("name"));
					GSN.vsbox.update($(this));
			});
		}});
	},
	menu: function (vsName) {
		GSN.debug("menu:"+vsName);
		if ($("#map").size()>0){
			//we are in the map context
			$("#vs").empty();
			GSN.addandupdate(vsName);
		} else {
			//we are in the normal context
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
								      	$.A({"href":"javascript:GSN.vsbox.remove('"+vsName+"');"},"close")
									  	//$.A({"class":"freeze","href":"javascript:GSN.freezevs('"+vsdiv+"');"},"freeze")
									    ),
									  $.DL({})
									  ));

			}
		}
		,update: function (vs){
			var vsdiv = "vsbox-"+vs.attr("name");
			//var id = $("#"+vsid+" span.id", where);
			//var status = $("#"+vsid+" span.status", where);
			
			var dl = $("#"+vsdiv+" > dl ", $(this.container));
			$("field",vs).each(function(){ 
				var name = $(this).attr("name");
				var value = $(this).text();
			
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
						$("."+name, dl).empty().append('<a href="'+value+'">download binary</a>');
					else
						$("a",$("."+name, dl)).attr("href",value);
				} else {
					if ($("."+name, dl).text() != value) {
						GSN.debug("Update: "+$("."+name, dl).text()+"->"+value);
						$("."+name, dl).empty().append(value);
					}
				}
			});
		}
		,remove: function (vsName) {
			var vsdiv = "vsbox-"+vsName;
			$("#"+vsdiv).remove();
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
 