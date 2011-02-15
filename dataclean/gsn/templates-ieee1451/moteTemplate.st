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
   <storage  history-size="60s"/>
   <streams>
         <stream name="mote_data"  >
                <source alias="mote"  sampling-rate="1" storage-size="1" >
                        <address wrapper="tinyos1x">
                                <predicate key="HOST">localhost</predicate>
                                <predicate key="RATE">1000</predicate>
                                <predicate key="PORT">9001</predicate>
                                <predicate key="TotalFields">$CH0:{c|$c.NumberOfChannels$}$</predicate>
    		$FIELD:{f| <predicate key="field.$f.key$">$f.name$ | $f.type$ | $f.description$ | get_$f.name$</predicate>}; separator="\n"$
   		        </address>
                        <query>select * from WRAPPER</query>
                </source>
                <query>select * from mote</query>
       </stream>
     </streams>
 </virtual-sensor>
