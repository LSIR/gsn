<virtual-sensor name="$CH0:{c|$c.name$}$" priority="11">
   <processing-class>
      <class-name>gsn.vsensor.BridgeVirtualSensor</class-name>
      <init-params/>
   </processing-class>
   <author>$CH0:{c|$c.author$}$</author>
   <email>$CH0:{c|$c.email$}$</email>
   <description>$CH0:{c|$c.description$}$</description>
   <life-cycle pool-size="10"/>
   <addressing>
      <predicate key="geographical">$CH0:{c|$c.location$}$</predicate>
   </addressing>
   <output-structure>
      $FIELD:{f| <field name="$f.name$"     type="$f.type$"/>}; separator="\n"$
   </output-structure>
   <storage  permanent-storage="false" history-size="60s"/>
   <input-streams>
         <input-stream name="mote_data"  >
                <stream-source alias="mote"  sampling-rate="1" storage-size="1" >
                        <address wrapper="tinyos">
                                <predicate key="HOST">192.168.51.13</predicate>
                                <predicate key="RATE">1000</predicate>
                                <predicate key="PORT">9001</predicate>
                                <predicate key="TotalFields">$CH0:{c|$c.NumberOfChannels$}$</predicate>
    		$FIELD:{f| <predicate key="field.$f.key$">$f.name$ | $f.type$ | $f.description$ | get_$f.name$</predicate>}; separator="\n"$
   		        </address>
                        <query>select * from "WRAPPER"</query>
                </stream-source>
                <query>select * from "mote"</query>
       </input-stream>
     </input-streams>
 </virtual-sensor>
