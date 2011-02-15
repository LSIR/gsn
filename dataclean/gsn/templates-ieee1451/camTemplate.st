<virtual-sensor name="$CH0:{c|$c.name$}$" priority="11">
   <processing-class>
      <class-name>gsn.vsensor.BridgeVirtualSensor</class-name>
      <init-params/>
       <output-structure>
      $FIELD:{f| <field name="$f.name$"     type="$f.type$"/>}; separator="\n"$
   </output-structure>
   </processing-class>
   <description>$CH0:{c|$c.description$}$</description>
   <life-cycle pool-size="10"/>
   <addressing>
      <predicate key="geographical">$CH0:{c|$c.location$}$</predicate>
   </addressing>
   <storage history-size="60s"/>
   <streams>
        <stream name="camera"  >
      		 <source alias="cam"   sampling-rate="1" storage-size="1" >
       		 <address wrapper="http-get">
        	   <predicate key="url">http://$CH0:{c|$c.IP$}$/axis-cgi/jpg/image.cgi?resolution=320x240&amp;clock=0&amp;date=0</predicate>
               <predicate key="RATE">1000</predicate>
               <predicate key="TotalFields">$CH0:{c|$c.NumberOfChannels$}$</predicate>
    		$FIELD:{f| <predicate key="field.$f.key$">$f.name$ | $f.type$ | $f.description$ | get_gsnHeader_$f.name$</predicate>}; separator="\n"$
   		    </address>
            <query>select DATA as $FIELD:{f|$f.name$}$ ,TIMED from WRAPPER</query>
			 </source>
         <query>select * from cam</query>
       </stream>
     </streams>
 </virtual-sensor>
