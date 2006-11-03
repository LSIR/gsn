/*
 * gsn javascript
 */
 
var GSN = { 
	updateall: function (createvs){
		if($("#refreshall_timeout").attr("value") > 0 ) {
			setTimeout('GSN.updateall(false);', $("#refreshall_timeout").attr("value" ));
		} else if(!createvs) return;
  
		//console.info("updateall query");  
		$.ajax({ type: "GET", url: "/gsn", success: function(data){
			//console.info("ajax successful");
			//console.debug($("rsp",data).attr("stat"));
			//if ($("rsp",data).attr("stat")!="ok") {
				//console.debug("Error: " + $("err",data).attr("msg")); 
			//} else {
				//$("#vs").empty();
				//console.info("ajax");
				$("virtual-sensor",data).each(function(){
					if (createvs) GSN.addvs($(this).attr("name"))
					GSN.updatevs($(this),$("#vs"))
				})
			//}
		}});
	},
	menu: function (vsName) {
		if ($("#map").size()>0){
			//we are in the map context
			$("#vs").empty();
			GSN.addvs(vsName);
		} else {
			//we are in the normal context
			GSN.addvs(vsName);
		}
	},
	addvs: function (vsName) {
		var vsdiv = "vs-"+vsName;
		if ($("#"+vsdiv, $("#vs")).size()==0) {
			//console.debug("create vs:"+vsdiv);
			$("#vs").append($.DIV({"id":vsdiv,"class":"vsbox"},
							$.H3({},$.SPAN({},vsName,
								$.SPAN({"class":"id"},"0"),
								$.SPAN({"class":"status"},"live")),
								$.A({"href":"javascript:GSN.removevs('"+vsdiv+"');"},"close"),
								$.A({"class":"freeze","href":"javascript:GSN.freezevs('"+vsdiv+"');"},"freeze")
								
							)
//							,$.P({},$.A({href:"javascript:console.debug('"+vsdiv+"');"},"dynamic data:"))
							,$.DL({})
//							,$.P({class:"clear"},"finish")
							)
							);
			$.ajax({ type: "GET", url: "/gsn?name="+vsName, success: function(data){
			//if ($("rsp",data).attr("stat")!="ok") {
			//	console.debug("Error: " + $("err",data).attr("msg")); 
			//} else {
				$("virtual-sensor",data).each(function(){
					GSN.updatevs($(this),$("#vs"))
				})
			//}
			}});
		}
	},
	updatevs: function (vs,where){
		var vsid = "vs-"+vs.attr("name");
		//console.debug("update: "+vsid);
		var id = $("#"+vsid+" span.id", where);
		var status = $("#"+vsid+" span.status", where);
		//if (status.text()!="live") return;
		//update id
		//id.empty().append(vs.attr("id"));
		$("field",vs).each(function(){ 
			var dl = $("#"+vsid+" > dl ", where);
			var name = $(this).attr("name");
			var value = $(this).text();
			if (value == "") value = "empty";
			if ($(this).attr("type").indexOf("image") != -1){
//				var img = $.IMG({"id":"test","alt":"error"});
				value = '<img src="'+value+'" alt="error" />';
//				where.append('<img src="'+value+'" alt="error" />');
				/*console.debug(value);
				$("#test").attr("src",value);
				console.debug($("#test").attr("src"));*/
			}
			if ($("."+name, dl).size()==0){
				//create name:value
				dl.append($.DT({},name));
				dl.append($.DD({"class":name}));
			} 
			//update value
			$("."+name, dl).empty().append(value);
		});
	
	},
	changevs: function (vsid,df) {
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
	},
	removevs: function (vsid) {
		//console.debug(vsid);
		$("#"+vsid).remove();
	}
};
 