<virtual-sensor name="$CH0:{c|$c.name$}$" priority="11">
   <processing-class>
      <class-name>gsn.vsensor.BridgeVirtualSensor</class-name>
      <output-structure>
      $FIELD:{f| <field name="$f.name$"     type="$f.type$"/>}; separator="\n"$
   </output-structure>
   </processing-class>
   <description>$CH0:{c|$c.description$}$</description>
   <life-cycle pool-size="10"/>
   <addressing>
      <predicate key="geographical">$CH0:{c|$c.location$}$</predicate>
   </addressing>
   
   <storage permanent-storage="true" history-size="15m" />
   <streams>
      <stream name="data" rate="100">
         <source alias="wsn" sampling-rate="1" storage-size="1"
                        disconnect-buffer-size="10">
            <address wrapper="tinynode">
               <predicate key="HOST">localhost</predicate>
               <predicate key="PORT">9001</predicate>

	       $FIELD:{f| <predicate key="field.$f.key$">$f.name$ | $f.type$ | $f.description$ | get_gsnHeader_$f.name$</predicate>}; separator="\n"$

            </address>
            <query>select * from WRAPPER</query>
         </source>
         <query>select * from wsn</query>
      </stream>
   </streams>
</virtual-sensor>
