//
// (c) 2013 ETH Zurich, Computer Engineering and Networks Laboratory
//

Date.prototype.formatTime = function() {
    expandNull = function(z, digits) {
        z = ""+z;
        for (var i = z.length; i<digits;i++)
        z = "0"+z;
        return z;
    }
    if (!this)
    return "";
    else {   
        return (1900+this.getYear())+"-"+expandNull(this.getMonth()+1,2)+"-"+expandNull(this.getDate(),2)+" "+expandNull(this.getHours(),2)+":"+expandNull(this.getMinutes(),2)+":"+expandNull(this.getSeconds(),2)+" "+(this.getTimezoneOffset()<0?"+":"")+(-this.getTimezoneOffset()/60)+":00";
    }  
}
function TimeSlider(initobject) {
  
  this.changedCallback = initobject.changedCallback;
  this.timeSliderWidth = initobject.width;
  this.timeSliderInfoHtmlElement = initobject.infoHtmlElement;
  this.minwidth = initobject.minwidth>0?initobject.minwidth:20;
  this.minRangeDefault = 3600000 * 6;
  this.minRange = this.minRangeDefault;
  
  this.timeSliderContainment = document.createElement('div');
  $(this.timeSliderContainment).css({ 'position':'relative', 'padding':0, 'width': this.timeSliderWidth+2 + 'px', 'height': '20px'});
  var timeSlider = document.createElement('div');
  $(timeSlider).css({ 'position':'absolute', 'top':'4px', 'left':'-1px', 'width': (this.timeSliderWidth+2) + 'px', 'height':'10px', 'z-index':'0' });
  timeSlider.className = 'ui-slider ui-slider-horizontal ui-widget ui-widget-content ui-corner-all';
  this.timeSliderMark = document.createElement('div');
  $(this.timeSliderMark).css({ 'width':this.timeSliderWidth+'px', 'height': '18px', 'margin': '0', 'top': '0', 'z-index':'1'});
  this.timeSliderMark.className = 'ui-slider-handle ui-state-default';
  this.timeSliderContainment.appendChild(this.timeSliderMark);
  this.timeSliderMarkWidth = this.timeSliderWidth;
  var grabber = document.createElement('div');
  grabber.className='ui-icon ui-icon-grip-solid-vertical';
  $(grabber).css({'margin-left':'auto', 'margin-right':'auto'});
  this.timeSliderMark.appendChild(grabber);
  this.timeSliderContainment.appendChild(timeSlider);
  var obj = this;
         
  $(this.timeSliderMark).draggable({
    axis: "x",
    containment: obj.timeSliderContainment,
    stop: function(event, ui) {
      var range = obj.getRange(ui);
      obj.changedCallback(range.mindate, range.maxdate);
   },
   drag: function(event, ui) {
     var range = obj.getRange(ui);
     obj.updateInfo(obj.getRange(ui));
    } 
  }).resizable({                
   containment: obj.timeSliderContainment,
   handles: 'e,w',
   minWidth: this.minwidth,
   stop: function(event, ui) {
     var range = obj.getRange(ui);
     obj.changedCallback(range.mindate, range.maxdate);
     },
   resize: function(event, ui) {
     obj.timeSliderMarkWidth=ui.size.width;
     var range = obj.getRange(ui);
     obj.updateInfo(obj.getRange(ui));
   }
  });

  this.updateInfo  = function (range) {
    $(this.timeSliderInfoHtmlElement).empty().html(
        range.mindate.formatTime() +" - " + range.maxdate.formatTime()
     );
  }
  
  this.getHtmlElement = function () {
    return this.timeSliderContainment;
  }
  
  // range in starttime, stoptime
  this.setRange = function (start, end) {
      // available values for calculation:
      // this.timeSliderMarkWidth: current with of the slider mark
      // this.minwidth: minimal width it can have
      // this.timeSliderWidth: maximal width the slider mark can have
      // ui.position.left: the start position of the slider mark
      //
      // this.minRange:
      // this.fullDataRange:
      // this.fullDataMin: t0
      
      // cut overflow
      var t_start = Math.max((start - this.fullDataMin), 0);
      var t_end = Math.min(end - this.fullDataMin, this.fullDataRange);
      
      var s_width = ((t_end - t_start)- (this.minRange * this.timeSliderWidth - this.fullDataRange * this.minwidth) / (this.timeSliderWidth - this.minwidth))
        * (this.timeSliderWidth - this.minwidth) / (this.fullDataRange - this.minRange);
        
      var Tr = this.fullDataRange - (t_end - t_start); // remaining time in time domain
      var Sr = this.timeSliderWidth - s_width; // remaining time in slider domain
      
      var s_min = (Tr>0?(Sr * t_start / Tr):0);
      var s_max = s_min + s_width;
      if (s_max - s_min < this.minwidth) {
        var adjust = (this.minwidth - (s_max - s_min))/2;
        s_max+=adjust;
        s_min-=adjust;
        if (s_min < 0) {
          s_min=0;
          s_max = s_min + this.minwidth;
        }
        else if (s_max > this.timeSliderWidth) {
          s_max = this.timeSliderWidth;
          s_min = s_max - this.minwidth;
        }
        
      }
    this.timeSliderMarkWidth = Math.round(s_max - s_min);
    if(this.timeSliderMarkWidth > this.timeSliderWidth) {
        this.timeSliderMarkWidth = this.timeSliderWidth;
    }
    $(this.timeSliderMark).css('left', Math.round(s_min));
    $(this.timeSliderMark).css('width', this.timeSliderMarkWidth);        
    this.updateInfo({mindate : new Date(this.fullDataMin+t_start),  maxdate : new Date(this.fullDataMin+t_end)});
  }
  
  // maxrange in starttime, stoptime
  this.setMaxRange = function (start, end) {
    obj.fullDataMin = start;
    obj.fullDataMax = end;
    obj.fullDataRange = end - start;
    if (obj.fullDataRange < obj.minRangeDefault * 10)
      obj.minRange = obj.fullDataRange / 10;
    else
      obj.minRange = obj.minRangeDefault;
  }
  
  this.getRange = function (ui) {
      // linear scale
      
      // the returned range should be 
      // min the minRange in the case where the slider is minimal
      // max the full Range in case where the slider has the full width
      
      // available values for calculation:
      // this.timeSliderMarkWidth: current with of the slider mark
      // this.minwidth: minimal width it can have
      // this.timeSliderWidth: maximal width the slider mark can have
      // ui.position.left: the start position of the slider mark
      //
      // this.minRange:
      // this.fullDataRange:
      // this.fullDataMin: t0
      
      // cut overflow
      var s_start = Math.max(ui.position.left, 0);
      var s_end = Math.min(ui.position.left + this.timeSliderMarkWidth, this.timeSliderWidth);
      if ((s_end - s_start) < this.minwidth) {
        if (s_end - this.minwidth > 0)
          s_start = s_end - this.minwidth;
        else
          s_end = s_start + this.minwidth;
      }
      
      var width = (this.minRange * this.timeSliderWidth - this.fullDataRange * this.minwidth) / (this.timeSliderWidth - this.minwidth)
        + (s_end - s_start) * (this.fullDataRange - this.minRange) / (this.timeSliderWidth - this.minwidth);
        
      var Tr = this.fullDataRange - width; // remaining time in time domain
      var Sr = this.timeSliderWidth - (s_end - s_start); // remaining time in slider domain
      
      var mindate = this.fullDataMin + (Sr>0?(Tr * ui.position.left / Sr):0);
      var maxdate = mindate + width;
      
      var mindate_unix = mindate;
      var maxdate_unix = maxdate;
      
      mindate = new Date(mindate);
      maxdate = new Date(maxdate);
      
    return {
      mindate : mindate,
      maxdate : maxdate
    };
  }    
  
}

function SignalSelect(initobject) {
    this.config = initobject.config;
    this.signalElements = new Array();
    this.topdiv = document.createElement('div');
    $(this.topdiv).css({'position':'relative','top':'-45px','left':'60px','width':(this.config.graph.width-70)+'px','z-index':'20','display':'none'});
    this.signalSelectDiv = document.createElement('div');
    this.topdiv.appendChild(this.signalSelectDiv);
    this.parentGraph = initobject.parent;
    $(this.signalSelectDiv).css({'position':'absolute','padding':'5px','width':(this.config.graph.width-70)+'px' ,'background-color':'#f0f0f0', 'border-bottom-left-radius': '4px', 'border-bottom-right-radius': '4px'});
    //$(this.signalSelectDiv).attr('class','ui-widget-content ui-corner-bottom');
    this.xValueDiv = document.createElement('div');
    $(this.xValueDiv).text('Selected time: -');
    this.signalSelectDiv.appendChild(this.xValueDiv);
    this.shownSignals = null;
    this.timezoneOffsetMsec = null;
    var timezoneDate = new Date();
    this.timezoneOffsetMsec = timezoneDate.getTimezoneOffset()*60*1000;
    
    if(typeof this.config.selectedSignalIdx != 'undefined') {
        var visibleCnt = 0;
        this.shownSignals = new Array();
        for (i=0; i < this.config.signals.length; i++) {
            if(typeof this.config.selectedSignalIdx != 'undefined') {
                if(!this.config.signals[i].visible) {
                    continue;
                }
                if(visibleCnt != this.config.selectedSignalIdx) {
                    visibleCnt++;
                    continue;
                }
                this.shownSignals.push(this.config.signals[i]);
                visibleCnt++;
            }
        }
    } else {
        this.shownSignals = this.config.signals;
    }
    
    var obj = this;
    var lastGroup = '';
    var container = null;
    for (i=0; i < this.shownSignals.length; i++) {
        curField = this.shownSignals[i].dataField;
        if(typeof curField == 'object') {
            curField = curField.valueOf();
        }
        if(curField != lastGroup) {
           if(container != null) {
                  this.signalSelectDiv.appendChild(container);
           }
           newElement = document.createElement('div');
           //$(newElement).text(curField);
           $(newElement).text(this.shownSignals[i].displayName);
           $(newElement).css({'clear':'left','float':'none'});
           this.signalSelectDiv.appendChild(newElement);
           lastGroup = curField;
           container = document.createElement('div');
           $(container).css({'position':'relative','width':(this.config.graph.width-70)+'px'});
       }
       var sigElements = new Array();
       var signalDiv = document.createElement('div');
       $(signalDiv).attr('id', 'sig_'+i);
       $(signalDiv).css({'position':'relative','height':'20px', 'width':'170px', 'border':'1px solid black','margin-right':'5px','margin-top':'3px','float':'left'});
       $(signalDiv).click(function() {obj.signalClicked(this);});
       var newElement = document.createElement('input');
       newElement.setAttribute('type', 'checkbox');
       signalDiv.appendChild(newElement);
       sigElements.checkbox = newElement;
       newElement.setAttribute('name', 'select_'+this.config.graph.div);
       newElement.setAttribute('value', 'y1_'+i);
       if(this.shownSignals[i].visible) {
           newElement.checked = true;
       }
       newElement = document.createElement('span');
       var caption = '(no pos)';
       if(this.shownSignals[i].deviceSelect.type == 'single' && this.shownSignals[i].deviceSelect.field == 'position') {
           caption = 'Pos '+this.shownSignals[i].deviceSelect.value;
       } else if(this.shownSignals[i].deviceSelect.type == 'single' && this.shownSignals[i].deviceSelect.field == 'device_id') {
           caption = 'ID '+this.shownSignals[i].deviceSelect.value;
       }
       $(newElement).text(caption+': ');
       signalDiv.appendChild(newElement);
       sigElements.caption = newElement;
       newElement = document.createElement('span');
       $(newElement).text('-');
       signalDiv.appendChild(newElement);
       sigElements.valueField = newElement;
       container.appendChild(signalDiv);
       this.signalElements.push(sigElements);
    }
    this.signalSelectDiv.appendChild(container);
    var formDiv = document.createElement('div');
    $(formDiv).css({'position':'relative','width':(this.config.graph.width-70)+'px','clear':'left','top':'3px'});
    newElement = document.createElement('input');
    $(newElement).attr({'type':'button','value':'update'});
    $(newElement).css({'clear':'left'});
    $(newElement).click(function() {obj.updateSignals();});
    formDiv.appendChild(newElement);
    newElement = document.createElement('input');
    $(newElement).attr({'type':'button','value':'select all'});
    $(newElement).click(function() {obj.selectAllSignals();});
    formDiv.appendChild(newElement);
    newElement = document.createElement('input');
    $(newElement).attr({'type':'button','value':'select none'});
    $(newElement).click(function() {obj.deselectAllSignals();});
    formDiv.appendChild(newElement);
    this.signalSelectDiv.appendChild(formDiv);
    
    this.getHtmlElement = function() {
        return this.topdiv;
    };
    
    this.highlightCallback = function(caller, event, x, points, row) {
        $(this.topdiv).show();
        var j = 0;
        var d = new Date(x-this.timezoneOffsetMsec);
        $(this.xValueDiv).text('Selected time: '+d.formatTime());
        var labels = caller.graph.attr_('labels');
        for (i=0; i < this.shownSignals.length; i++) {
               if(!this.shownSignals[i].visible) {
                   $(this.signalElements[i].valueField).text('-');
                   $(this.signalElements[i].caption).css('color', 'black');
                   $(this.signalElements[i].valueField).css('color', 'black');
               } else {
                   var unit = '';
                   if(this.shownSignals[i].unit != null && this.shownSignals[i].unit != '') {
                       unit = this.shownSignals[i].unit;
                   }
                   var point = null;
                   for (var p in points) {
                     if (points[p].name == this.shownSignals[i].displayName) {
                       point = points[p];
                       break;
                     }
                   }
                   if (point!=null) 
                     $(this.signalElements[i].valueField).text(Dygraph.round_(point.yval,2)+' '+unit);
                   else
                     $(this.signalElements[i].valueField).text('-');
                   if (caller.graph.visibility()[j]) {
                       c = caller.graph.plotter_.colors[labels[j+1]];
                       $(this.signalElements[i].caption).css('color', c);
                       $(this.signalElements[i].valueField).css('color', c);
                   } else {
                       $(this.signalElements[i].caption).css('color', 'black');
                       $(this.signalElements[i].valueField).css('color', 'black');
                   }
                   j++;                   
               }
        }
    };
    this.signalClicked = function(caller) {
        idx = $(caller).attr('id').split('_')[1];
        if(this.shownSignals[idx].visible) {
            this.shownSignals[idx].visible = false;
            this.signalElements[idx].checkbox.checked = false;
        } else {
            this.shownSignals[idx].visible = true;
            this.signalElements[idx].checkbox.checked = true;
        }
    };
    
    this.updateSignals = function() {
        for (i=0; i < this.shownSignals.length; i++) {
               if(!this.shownSignals[i].visible) {
                   $(this.signalElements[i].valueField).text('-');
                   $(this.signalElements[i].caption).css('color', 'black');
                   $(this.signalElements[i].valueField).css('color', 'black');
               }
        }
        this.parentGraph.updatePlot(true);
    };
    
    this.selectAllSignals = function() {
        for (i=0; i < this.shownSignals.length; i++) {
            this.shownSignals[i].visible = true;
            this.signalElements[i].checkbox.checked = true;
        }
    };
    
    this.deselectAllSignals = function() {
        for (i=0; i < this.shownSignals.length; i++) {
            this.shownSignals[i].visible = false;
            this.signalElements[i].checkbox.checked = false;
        } 
    };
}

function VizzlyDygraph() {
    this.graph = null;
    this.selectRangeStart = null;
    this.selectRangeEnd = null;
    this.config = null;
    this.element = null;
    this.lastClick = null;
    this.timeSlider = null;
    this.loadingCallback = null;
    this.fullDataTimeRange = null;
    this.timeSliderWidth = null;
    this.labelsDiv = null;
    this.zoomTextDiv = null;
    this.signalSelect = null;
    this.selectedSignalIdx = null;
    this.forceLoadUnaggregated = false;
    this.yaxisrange=[];
    this.timezoneOffsetMsec = null;
    this.init = function(config, element, mousediv) {
         this.config = config;
         var timezoneDate = new Date();
         this.timezoneOffsetMsec = timezoneDate.getTimezoneOffset()*60*1000;
         
         this.element = document.createElement('div');

         $(this.element).css({ 'height':this.config.graph.height+'px', 'width':this.config.graph.width+'px', 'z-index':'1' });
         element.appendChild(this.element);
         
         this.errorDiv = document.createElement('div');
         $(this.errorDiv).css({ 'position':'relative', 'top':'-200px', 'left':'30px', 'width': (this.config.graph.width-20)+'px', 'height':'20px', 'z-index':'100', 'visibility':'hidden', 'padding':'5px', 'text-align':'center' });
         element.appendChild(this.errorDiv);
         
         var obj = this;
         this.sliderInfo = document.createElement('div');
         this.sliderInfo.style.width = '350px';
         this.sliderInfo.style.height = '20px';
         this.sliderInfo.style.position = 'relative';
         this.sliderInfo.style.top = '-18px';
         this.sliderInfo.style.left = (obj.config.graph.width - 360)+'px';
         this.sliderInfo.style.fontSize = '8pt';
         this.sliderInfo.style.textAlign = 'right';

         this.timeslider = new TimeSlider({
           changedCallback : function(mindate, maxdate) {
             obj.selectRangeStart = mindate;
             obj.selectRangeEnd = maxdate;
             obj.updatePlot(true);
           },
           width : obj.config.graph.width-110,
           infoHtmlElement: obj.sliderInfo
         });
         var slider = this.timeslider.getHtmlElement();
         
         // Div for diplaying zoom links
         this.zoomTextDiv = document.createElement('div');
         this.zoomTextDiv.style.width = '230px';
         this.zoomTextDiv.style.height = '20px';
         this.zoomTextDiv.style.position = 'relative';
         this.zoomTextDiv.style.top = '0px';
         this.zoomTextDiv.style.left = '60px';
         this.zoomTextDiv.style.fontSize = '8pt';
         element.appendChild(this.zoomTextDiv);
         
         // Div for showing selected time span
         $(this.sliderInfo).css('top', '-20px');
         element.appendChild(this.sliderInfo);
         
         // Add slider with two buttons
         var buttonLeft = document.createElement('a');
         $(buttonLeft).html("&laquo;").attr("href", "").css({'position':'relative','top':'-20px','left':'60px','height':'20px'})
         .click(function() { obj.panButton('left'); return false; });
         element.appendChild(buttonLeft);
         $(slider).css({'left':'80px', 'top':'-35px'});
         element.appendChild(slider);
         var buttonRight = document.createElement('a');
         $(buttonRight).html("&raquo;").attr("href", "").css({'position':'relative','top':'-55px', 'left':(obj.config.graph.width-15)+'px', 'height':'20px'})
         .click(function() { obj.panButton('right'); return false; });
         element.appendChild(buttonRight);
      
         // Div for displaying plot values
         this.labelsDiv = document.createElement('div');
         $(this.labelsDiv).css({'visibility':'hidden','width':'1px','height':'1px'});
         element.appendChild(this.labelsDiv);
         
         this.signalSelect = new SignalSelect({
             config: this.config,
             parent: obj
         });
         element.appendChild(this.signalSelect.getHtmlElement());
         var obj = this;
         $(mousediv).bind('mouseleave', function() {
           $(obj.signalSelect.getHtmlElement()).hide();
         });
         
         if (this.config.inittimerange) {
          this.selectRangeEnd = new Date();
          this.selectRangeStart = new Date(this.selectRangeEnd.getTime()-(this.config.inittimerange));
         }
         if(this.config.selectRangeStart) {
             this.selectRangeStart = this.config.selectRangeStart;
         }
         if(this.config.selectRangeEnd) {
              this.selectRangeEnd = this.config.selectRangeEnd;
         }
         if(typeof this.config.selectedSignalIdx != 'undefined') {
              this.selectedSignalIdx = this.config.selectedSignalIdx;
         }
         
         this.updatePlot(true);
    };
    this.registerLoadingCallback = function(callback) {
        this.loadingCallback = callback;
    };
    this.updatePlot = function(fullReload) {
        this.clearErrorMessage();
        this.loadingCallback(true);
        var caller = this;
        this.config.loaddata(fullReload, this.config.signals, this.selectRangeStart, this.selectRangeEnd, function(fullReload, data) {caller.dataLoadedCallback(fullReload, data)});
    };
    this.dataLoadedCallback = function(fullReload, data) {
      var errorFromServer = data.match(/# ERROR:\s*.*/g);
      if(errorFromServer == null) {
          var range = data.match(/#\s*[-]*\d*,\s*[-]*\d*/)[0].match(/[-]*\d+/g);
          if(range[0] == -1) {
              this.showErrorMessage("Plot is empty. Try selecting other signals, if available.", false);
              this.loadingCallback(false);
          } else {
            this.fullDataTimeRange = new Array(parseInt(range[0]), parseInt(range[1]));
            this.timeslider.setMaxRange(this.fullDataTimeRange[0], this.fullDataTimeRange[1]);
            if (this.selectRangeStart==null && this.selectRangeEnd==null) {
              this.yaxisrange=[];
              this.selectRangeStart=new Date(this.fullDataTimeRange[0]);
              this.selectRangeEnd=new Date(this.fullDataTimeRange[1]);
            }
            this.timeslider.setRange(this.selectRangeStart,this.selectRangeEnd);
            if(data.split("\n").length <= 3) { // && data.split("\n")[2].length == 0) {
                this.showErrorMessage("Selected time range contains no data. Please select another time range.", true);
                this.loadingCallback(false);
            } else if(data.split("\n").length == 4) {
                this.showErrorMessage("There is only a single (not displayable) point in the selected time range.", true);
                this.loadingCallback(false);
            }
            this.realUpdatePlot(fullReload, data);
          }
      } else {
        this.showErrorMessage("(Server) " + errorFromServer[0].replace(/# ERROR:/, ""), false);
        this.loadingCallback(false);
      }
    };
    this.realUpdatePlot = function(fullReload, data) {
        if(fullReload) {
            var obj = this;
            var options = {
                    zoomCallback: function(minDate, maxDate, yRanges){obj.zoomCallback(minDate, maxDate, yRanges);},
                    clickCallback: function(e, x, pts){obj.clickCallback(e, x, pts);},
                    xAxisLabelWidth: 90,
                    yAxisLabelWidth: 80,
                    pixelsPerXLabel: 90,
                    xAxisLabelFormatter: function(d, gran){return obj.xAxisFormatter(d, gran)},
                    labelsDiv: this.labelsDiv,
                    highlightCallback: function(event, x, points,row){obj.signalSelect.highlightCallback(obj, event, x, points,row);},
                    connectSeparatedPoints: true
            };
            if (this.yaxisrange.length>0) {
              options.valueRange = this.yaxisrange;
            }
            if (this.graph) { this.graph.destroy(); }
            this.graph = new Dygraph(this.element, data, options);
        } else {
          var options = { 'file': data };
          if (this.yaxisrange.length>0)
            options.valueRange = this.yaxisrange;
          this.graph.updateOptions(options);
        }
        this.xaxisrange = this.graph.xAxisRange();
        this.updateZoomTextDiv();
        this.loadingCallback(false);
        
        //var labels = this.graph.attr_('labels');
        //for (var i = 1; i < labels.length; i++) {
        //    if (!this.graph.visibility()[i - 1]) continue;
        //     var c = this.graph.plotter_.colors[labels[i]];
        //     console.log(c+' '+labels[i]);
        //}
        
    };
    this.zoomCallback = function(minDate, maxDate, yRanges) {
        if (minDate != this.xaxisrange[0] || maxDate!=this.xaxisrange[1]) {
          // All timestamps coming from Vizzly are in UTC
          minDate = minDate-this.timezoneOffsetMsec;
          maxDate = maxDate-this.timezoneOffsetMsec;
          this.timeslider.setRange(minDate, maxDate);
          this.selectRangeStart = new Date(minDate);
          this.selectRangeEnd = new Date(maxDate);
          this.updatePlot(false); 
        }
        else {
          this.yaxisrange=yRanges[0];
        }
    };
    this.clickCallback = function(e, x, pts) {
        if(this.lastClick == e.offsetX) {
            this.lastClick = null;
            this.selectRangeStart = null;
            this.selectRangeEnd = null;
            this.updatePlot(true);
        }
        this.lastClick = e.offsetX;
    };
    this.xAxisFormatter = function(date, gran) {
        // All timestamps coming from Vizzly are in UTC
        date.setTime(date.getTime()-this.timezoneOffsetMsec);
        if (gran >= Dygraph.DECADAL) {
          return date.strftime('%Y');
        } else if (gran >= Dygraph.MONTHLY) {
          return date.strftime('%b %Y');
        } else {
          var frac = date.getHours() * 3600 + date.getMinutes() * 60 + date.getSeconds() + date.getMilliseconds();
          if (frac == 0 || gran >= Dygraph.DAILY) {
            return new Date(date.getTime() + 3600*1000).strftime('%G-%m-%d');
          } else {
            return Dygraph.hmsString_(date.getTime());
          }
        }
    };
    this.showErrorMessage = function(msg, onlyWarning) {
        if(onlyWarning) {
            $(this.errorDiv).css({'border':'2px solid yellow', 'background-color':'#FFFFCC'});
            htmlMsg = '<b>WARNING:</b> ' + msg; 
        } else {
            $(this.errorDiv).css({'border':'2px solid red', 'background-color':'#FF9999'});
            htmlMsg = '<b>ERROR:</b> ' + msg; 
        }
        $(this.errorDiv).css('visibility','visible');
        $(this.errorDiv).html(htmlMsg);
    };
    this.clearErrorMessage = function() {
        $(this.errorDiv).css('visibility','hidden');
    };
    this.updateZoomTextDiv = function() {
        $(this.zoomTextDiv).children().remove();
        this.appendZoomTextLink(this.zoomTextDiv, "Max", this.fullDataTimeRange[0], this.fullDataTimeRange[1], false);
        var diff = Math.floor((this.fullDataTimeRange[1]-this.fullDataTimeRange[0])/(3600*24*1000));
        if(diff > 365*1.5) {
            bounds = this.calculateTextLinkBounds(365*24*3600*1000);
            this.appendZoomTextLink(this.zoomTextDiv, "1y", bounds[0], bounds[1], false);
        }
        if(diff > 90*1.5) {
            bounds = this.calculateTextLinkBounds(90*24*3600*1000);
            this.appendZoomTextLink(this.zoomTextDiv, "3m", bounds[0], bounds[1], false);
        }
        if(diff > 30*1.5) {
            bounds = this.calculateTextLinkBounds(30*24*3600*1000);
            this.appendZoomTextLink(this.zoomTextDiv, "1m", bounds[0], bounds[1], false);
        }
        if(diff > 7*1.5) {
            bounds = this.calculateTextLinkBounds(7*24*3600*1000);
            this.appendZoomTextLink(this.zoomTextDiv, "1w", bounds[0], bounds[1], false);
        }
        if(diff > 1.5) {
            bounds = this.calculateTextLinkBounds(1*24*3600*1000);
            this.appendZoomTextLink(this.zoomTextDiv, "1d", bounds[0], bounds[1], false);
        }
        d = new Date();
        if(this.fullDataTimeRange[1] >= d.getTime()-(12*3600*1000)) {
            this.appendZoomTextLink(this.zoomTextDiv, "Now", this.fullDataTimeRange[1]-(12*3600*1000),this.fullDataTimeRange[1], false);
        }
    };
    this.calculateTextLinkBounds = function(duration) {
        var center = (this.selectRangeEnd.getTime()-this.selectRangeStart.getTime())/2+this.selectRangeStart.getTime();
        var newMinDate = Math.floor(center-(duration/2));
        var newMaxDate = Math.floor(center+(duration/2));
        if(newMinDate < this.fullDataTimeRange[0]) {
            newMinDate = newMinDate + (this.fullDataTimeRange[0]-newMinDate);
            newMaxDate = newMaxDate + (this.fullDataTimeRange[0]-newMinDate);
        }
        if(newMaxDate > this.fullDataTimeRange[1]) {
            newMinDate = newMinDate - (newMaxDate-this.fullDataTimeRange[1]);
            newMaxDate = newMaxDate - (newMaxDate-this.fullDataTimeRange[1]);
        }
        return new Array(newMinDate, newMaxDate);
    };
    this.appendZoomTextLink = function(parentElement, caption, minDate, maxDate, forceLoadUnaggregated) {
        var h = document.createElement('a');
        var obj = this;
        $(h).text(caption).click(function() { obj.textZoom(minDate, maxDate, forceLoadUnaggregated); return false; });
        $(h).attr("href", "");
        $(parentElement).append(h);
        var s = document.createElement('span');
        $(s).text(' ');
        $(parentElement).append(s);
    };
    this.textZoom = function(minDate, maxDate, forceLoadUnaggregated) {
        this.timeslider.setRange(minDate, maxDate);
        this.selectRangeStart = new Date(minDate);
        this.selectRangeEnd = new Date(maxDate);
        this.forceLoadUnaggregated = forceLoadUnaggregated;
        this.updatePlot(true);
    };
    this.panButton = function(dir) {
      var timeSpan = this.selectRangeEnd.getTime()-this.selectRangeStart.getTime();
      if(dir == 'left') {
          var newMinDate = this.selectRangeStart.getTime()-Math.floor(0.75*timeSpan);
          var newMaxDate = this.selectRangeEnd.getTime()-Math.floor(0.75*timeSpan);
          if(newMinDate < this.fullDataTimeRange[0]) {
              newMinDate = newMinDate + (this.fullDataTimeRange[0]-newMinDate);
              newMaxDate = newMaxDate + (this.fullDataTimeRange[0]-newMinDate);
          }
      }  else {
          var newMinDate = this.selectRangeStart.getTime()+Math.floor(0.75*timeSpan);
          var newMaxDate = this.selectRangeEnd.getTime()+Math.floor(0.75*timeSpan);
          if(newMaxDate > this.fullDataTimeRange[1]) {
              newMinDate = newMinDate - (newMaxDate-this.fullDataTimeRange[1]);
              newMaxDate = newMaxDate - (newMaxDate-this.fullDataTimeRange[1]);
          }
      }
      this.textZoom(newMinDate, newMaxDate);
    };
    this.getVisibleSignals = function() {
        var visibleSignals = [];
        var visibleIdxCnt = 0;
        for(i=0; i < this.config.signals.length; i++) {
            if(this.config.signals[i].visible) {
                // if this.selectedSignalIdx is set, there is only one signal to be displayed
                if(this.selectedSignalIdx != null) {
                    if(visibleIdxCnt == this.selectedSignalIdx) {
                        visibleSignals.push(this.config.signals[i]);
                    }
                    visibleIdxCnt++;
                } else {
                    visibleSignals.push(this.config.signals[i]);
                }
            }
        }
        return visibleSignals;
    };
}

function LinePlotWidget(config) {
    this.graph = null;
    this.statusDiv = null;
    this.config = null;
    this.createDivs = function() {
      var container = (typeof this.config.graph.div == 'object') ? this.config.graph.div : document.getElementById(this.config.graph.div);
      container.className = 'vizzly';
      this.parentDiv = document.createElement('div'); // absolute pos
      $(this.parentDiv).css({'position':'absolute', 'width':(parseInt(this.config.graph.width)) + 'px', 'height':(parseInt(this.config.graph.height)+100) + 'px'});
      container.appendChild(this.parentDiv);
      
      container.style.height = (parseInt(this.config.graph.height)+100) + 'px';
      this.statusDiv = document.createElement('div');
      this.parentDiv.appendChild(this.statusDiv);
      var loadingBar = document.createTextNode("... loading ...");
      this.statusDiv.appendChild(loadingBar);
      this.statusDiv.className = 'status';
      this.statusDiv.style.visibility='hidden';
      this.statusDiv.style.width= this.config.graph.width + 'px';
      var graphDiv = document.createElement('div');
      this.parentDiv.appendChild(graphDiv);
      
      graphDiv.style.width = (parseInt(this.config.graph.width)+30) + 'px';
      graphDiv.style.height = (parseInt(this.config.graph.height)+30) + 'px';
      this.initDygraph(graphDiv);
    };
    this.initDygraph = function(graphDiv) {
        this.graph = new VizzlyDygraph(graphDiv);
        var obj = this;
        this.graph.registerLoadingCallback(function(isLoading){obj.setLoading(isLoading)});
        this.graph.init(this.config, graphDiv, this.parentDiv);
    };
    this.setLoading = function(isLoading) {
        if(isLoading) this.statusDiv.style.visibility='visible';
        else this.statusDiv.style.visibility='hidden';
    };
    this.config = config;
    this.createDivs();
}
