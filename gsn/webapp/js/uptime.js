var uptime = { 

  s : 0,
  visible : false,

  update : function(){
    uptime.s = uptime.s+1000;
    var out = "uptime: ";
    if (uptime.s > 86400000)
      out = out + Math.floor(uptime.s / 86400000)+"d ";
    if (uptime.s > 3600000)
      out = out + Math.floor((uptime.s % 86400000) / 3600000)+"h ";
    if (uptime.s > 60000)
      out = out + Math.floor(((uptime.s % 86400000) % 3600000 )/ 60000)+"m ";
    out = out + Math.floor((((uptime.s % 86400000) % 3600000 ) % 60000)/1000)+"s";

    if (this.visible)
       $("#gsn-uptime").text(out);
    setTimeout("uptime.update()",1000); 
  },

  setTime : function(time) {
    uptime.s = Math.floor(time / 1000) * 1000;
    setTimeout("uptime.update()",1000 - time % 1000); 
  }

};

$(document).ready(function() {
  $("#footer").append("<div style=\"position:relative\"><div id=\"gsn-uptime\" style=\"position:absolute; bottom:-25px; right:0px; text-align:right\"></div></div>");
  uptime.visible = true;
});