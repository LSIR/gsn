<style type="text/css">
.vsbox {
		clear:both;
		margin:0;
		margin-bottom:5px;
		padding:1px;
		background:#FFA84C;
		}
.vsbox h3 {
		margin:3px;
		padding:1px;
		xdisplay:block;
		}
.vsbox h3 span {
		margin-bottom:3px;
		float:left;
		}
.vsbox h3 span.id,.vsbox h3 span.status {
		font-size:0.7em;
		float:none;
		display:none;
		}
.vsbox h3 a {
		float:right;
		color:#000000;
		margin:0 2px;
		}		
.vsbox p,.vsbox dl {
	clear:both;
	padding:1px;
	margin:0;
	background:#ffffff;
}		
.vsbox dt {
	clear:both;
	float:left;
	font-weight:bold;
	padding-right:5px;
}
.vsbox dd {
	xfloat:left;
}
.clear {
		clear:both;
}
</style>
<script type="text/javascript">
<!--//<![CDATA[

if (!window.console) {
  window.console = {
    timers: {},
    openwin: function() {
      window.top.debugWindow =
          window.open("",
                      "Debug",
                      "left=0,top=0,width=300,height=700,scrollbars=yes,"
                      +"status=yes,resizable=yes");
      window.top.debugWindow.opener = self;
      window.top.debugWindow.document.open();
      window.top.debugWindow.document.write('<html><head><title>debug window</title></head><body><hr /><pre>');
    },

    debug: function(entry) {
      window.top.debugWindow.document.write(entry+"\n");
    },

    time: function(title) {
      window.console.timers[title] = new Date().getTime();
    },

    timeEnd: function(title) {
      var time = new Date().getTime() - window.console.timers[title];
      console.log(['<strong>', title, '</strong>: ', time, 'ms'].join(''));
    }

  }

  if (!window.top.debugWindow) { console.openwin(); }
}


var GSN = { 
	updateall: function (createvs){
		if($("#refreshall_timeout").attr("value") > 0 ) {
			setTimeout('GSN.updateall(false);', $("#refreshall_timeout").attr("value" ));
		} else if(!createvs) return;
  
		$.ajax({ type: "GET", url: "api.jsp?vs=GPSVS", success: function(data){
			//console.debug($("rsp",data).attr("stat"));
			if ($("rsp",data).attr("stat")!="ok") {
				console.debug("Error: " + $("err",data).attr("msg")); 
			} else {
				//$("#vs").empty();
				$("virtualsensor",data).each(function(){
					if (createvs) GSN.addvs($(this).attr("name"))
					GSN.updatevs($(this),$("#vs"))
				})
			}
		}});
	},
	addvs: function (vsName) {
		var vsdiv = "vs-"+vsName;
		if ($("#"+vsdiv, $("#vs")).size()==0) {
			console.debug("create vs:"+vsdiv);
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
			$.ajax({ type: "GET", url: "api.jsp?vs="+vsName, success: function(data){
			if ($("rsp",data).attr("stat")!="ok") {
				console.debug("Error: " + $("err",data).attr("msg")); 
			} else {
				$("virtualsensor",data).each(function(){
					GSN.updatevs($(this),$("#vs"))
				})
			}
			}});
		}
	},
	updatevs: function (vs,where){

		var vsid = "vs-"+vs.attr("name");
			console.debug("update: "+vsid);
		var id = $("#"+vsid+" span.id", where);
		var status = $("#"+vsid+" span.status", where);
		if (status.text()!="live") return;
		//update id
		id.empty().append(vs.attr("id"));
		$("field",vs).each(function(){ 
			var dl = $("#"+vsid+" > dl ", where);
			var name = $(this).attr("name");
			var value = $(this).text();
			if ($("."+name, dl).size()==0){
				//create name:value
				dl.append($.DT({},name));
				dl.append($.DD({"class":name},value));
			} else {
				//update value
				$("."+name, dl).empty().append($(this).text());
			}
		});
	
	},
	changevs: function (vsid,df) {
		var id = $("#"+vsid+" > h3 > span > span.id");	
		newid = parseInt(id.text()) + df;
		id.empty().append(newid);
		console.debug("nothing: "+newid);
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
		console.debug(vsid);
		$("#"+vsid).remove();
	}
};


$(document).ready(function() {
	console.debug("javascript main.jsp init");
	$("#refreshall_timeout").bind("change",'GSN.updateall(false)');
	//$("#refreshall_enable").attr("checked","checked");
	GSN.updateall(true);
});	
//]]>-->
</script>
<h2>Global Sensor Network</h2>
<form><p>refresh every msec : 
<select id="refreshall_timeout" >
<option value="3600000">1hour</option> 
<option value="600000">10min</option> 
<option value="60000">1min</option> 
<option value="30000">30sec</option> 
<option value="5000" selected="selected">5sec</option> 
<option value="1000">1sec</option> 
<option value="0">disable</option> 
</select>
</p></form>
<div id="vs"></div>		