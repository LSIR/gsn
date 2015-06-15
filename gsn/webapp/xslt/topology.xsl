<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema">
<xsl:template match="/">
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2"
xmlns:atom="http://www.w3.org/2005/Atom">
<Document>
<StyleMap id="msn_purple-stars">
<Pair>
<key>normal</key>
<styleUrl>#sn_purple-stars</styleUrl>
</Pair>
<Pair>
<key>highlight</key>
<styleUrl>#sh_purple-stars</styleUrl>
</Pair>
</StyleMap>
<Style id="sh_purple-stars">
<IconStyle>
<scale>1.3</scale>
<Icon>
<href>http://maps.google.com/mapfiles/kml/paddle/purple-stars.png</href>
</Icon>
<hotSpot x="32" y="1" xunits="pixels" yunits="pixels"/>
</IconStyle>
<ListStyle>
<ItemIcon>
<href>http://maps.google.com/mapfiles/kml/paddle/purple-stars-lv.png</href>
</ItemIcon>
</ListStyle>
</Style>
<Style id="sn_purple-stars">
<IconStyle>
<scale>1.1</scale>
<Icon>
<href>http://maps.google.com/mapfiles/kml/paddle/purple-stars.png</href>
</Icon>
<hotSpot x="32" y="1" xunits="pixels" yunits="pixels"/>
</IconStyle>
<ListStyle>
<ItemIcon>
<href>http://maps.google.com/mapfiles/kml/paddle/purple-stars-lv.png</href>
</ItemIcon>
</ListStyle>
</Style>
<StyleMap id="msn_ylw-stars">
<Pair>
<key>normal</key>
<styleUrl>#sn_ylw-stars</styleUrl>
</Pair>
<Pair>
<key>highlight</key>
<styleUrl>#sh_ylw-stars</styleUrl>
</Pair>
</StyleMap>
<Style id="sh_ylw-stars">
<IconStyle>
<scale>1.3</scale>
<Icon>
<href>http://maps.google.com/mapfiles/kml/paddle/ylw-stars.png</href>
</Icon>
<hotSpot x="32" y="1" xunits="pixels" yunits="pixels"/>
</IconStyle>
<ListStyle>
<ItemIcon>
<href>http://maps.google.com/mapfiles/kml/paddle/ylw-stars-lv.png</href>
</ItemIcon>
</ListStyle>
</Style>
<Style id="sn_ylw-stars">
<IconStyle>
<scale>1.1</scale>
<Icon>
<href>http://maps.google.com/mapfiles/kml/paddle/ylw-stars.png</href>
</Icon>
<hotSpot x="32" y="1" xunits="pixels" yunits="pixels"/>
</IconStyle>
<ListStyle>
<ItemIcon>
<href>http://maps.google.com/mapfiles/kml/paddle/ylw-stars-lv.png</href>
</ItemIcon>
</ListStyle>
</Style>
<Style id="sn_ylw-pushpin">
<IconStyle>
<scale>1.1</scale>
<Icon>
<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>
</Icon>
<hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
</IconStyle>
<LineStyle>
<color>ff00ff00</color>
<width>3</width>
</LineStyle>
</Style>
<Style id="sh_ylw-pushpin">
<IconStyle>
<scale>1.3</scale>
<Icon>
<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>
</Icon>
<hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
</IconStyle>
<LineStyle>
<color>ff00ffff</color>
<width>2</width>
</LineStyle>
</Style>
<StyleMap id="msn_ylw-pushpin">
<Pair>
<key>normal</key>
<styleUrl>#sn_ylw-pushpin</styleUrl>
</Pair>
<Pair>
<key>highlight</key>
<styleUrl>#sh_ylw-pushpin</styleUrl>
</Pair>
</StyleMap>

  <xsl:for-each select="network/sensornodes/sensornode[@longitude][@latitude][@altitude]">
    <Placemark>
       <name><xsl:value-of select="@position"/></name>
       <description>
       <b>Permasense SensorNode</b><br/>
       <table id="datatable" style="margin-left:0px;padding:0px">
       <tr><td><b>Position</b></td><td><xsl:value-of select="@position"/></td></tr>
       <tr><td><b>Device Id</b></td><td><xsl:value-of select="@node_id"/></td></tr>
       <tr><td><b>Device Type</b></td><td>
       <xsl:choose>
        <xsl:when test="@nodetype = 1">SIB TinyNode</xsl:when>
	<xsl:otherwise>
		<xsl:choose>
		<xsl:when test="@nodetype = 2">WGPS TinyNode</xsl:when>
		<xsl:otherwise>
			<xsl:choose>
			<xsl:when test="@nodetype = 3">PowerSwitch TN</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
				<xsl:when test="@nodetype = 4">BaseStation</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
					<xsl:when test="@nodetype = 5">GPS Corestation</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
						<xsl:when test="@nodetype = 6">CamZilla CoreStation</xsl:when>
						<xsl:otherwise>
							<xsl:choose>
							<xsl:when test="@nodetype = 7">GPS Logger</xsl:when>
							<xsl:otherwise>
								<xsl:choose>
								<xsl:when test="@nodetype = 8">Webcam</xsl:when>
								<xsl:otherwise>
									<xsl:choose>
									<xsl:when test="@nodetype = 9">AE TinyNode</xsl:when>
									<xsl:otherwise>
										unknown
									</xsl:otherwise>
									</xsl:choose>
								</xsl:otherwise>
								</xsl:choose>
							</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
			</xsl:choose>
		</xsl:otherwise>
		</xsl:choose>
	</xsl:otherwise>
       </td></tr>
       <tr><td><b>Parent ID</b></td><td><xsl:value-of select="@parent_id"/></td></tr>
       <tr><td><b>Last packet generated at</b></td><td><xsl:value-of select="@generation_time"/></td></tr>
       <tr><td><b>Packet count</b></td><td><xsl:value-of select="@packet_count"/></td></tr>
       <tr><td><b>VSys</b></td><td><xsl:value-of select="@vsys"/>V</td></tr>
       <tr><td><b>VSdi</b></td><td><xsl:value-of select="@vsdi"/>V</td></tr>
       <xsl:choose>
        <xsl:when test="@nodetype = 1">
          <tr><td><b>Current</b></td><td><xsl:value-of select="@current"/>mA</td></tr>
        </xsl:when>
       </xsl:choose>
       <tr><td><b>Temperature</b></td><td><xsl:value-of select="@temperature"/>°C</td></tr>
       <tr><td><b>Humidity</b></td><td><xsl:value-of select="@humidity"/>%</td></tr>
       <tr><td><b>Flash count</b></td><td><xsl:value-of select="@flash_count"/></td></tr>
       <tr><td><b>uptime</b></td><td><xsl:value-of select="@uptime"/>s</td></tr>
       <tr><td><b>Battery level</b></td><td><xsl:value-of select="@batterylevel"/>%</td></tr>
       <xsl:choose>
        <xsl:when test="@nodetype = 5">
          <tr><td><b>Corestation state</b></td><td>
          <xsl:choose>
            <xsl:when test="@corestation_running = true">running</xsl:when>
            <xsl:otherwise>sleeping</xsl:otherwise>
          </xsl:choose>
          </td></tr>
        </xsl:when>
       </xsl:choose>
       <xsl:choose>
        <xsl:when test="@nodetype = 3">
          <tr><td><b>Port states</b></td><td>
          <xsl:choose>
            <xsl:when test="@p1 = true">on</xsl:when>
            <xsl:otherwise>off</xsl:otherwise>
          </xsl:choose>
          |
          <xsl:choose>
            <xsl:when test="@p2 = true">on</xsl:when>
            <xsl:otherwise>off</xsl:otherwise>
          </xsl:choose>
          </td></tr>
        </xsl:when>
       </xsl:choose>
       </table>
       </description>
       <styleUrl>#msn_purple-stars</styleUrl>
       <Point>
          <coordinates><xsl:value-of select="@longitude"/>,<xsl:value-of select="@latitude"/>,<xsl:value-of select="@altitude"/></coordinates>
       </Point>
    </Placemark>
  </xsl:for-each>
  <Folder><name>Network</name><visibility>0</visibility><open>0</open>
  <xsl:for-each select="network/sensornodes/sensornode[(@parent_id!=@node_id)][@longitude][@latitude][@altitude]">
    <xsl:variable name="parent_id" select="@parent_id"/>
    <Placemark>
      <styleUrl>#msn_ylw-pushpin</styleUrl>
      <LineString>
        <extrude>0</extrude>
        <tessellate>0</tessellate>
        <altitudeMode>clampToGround</altitudeMode>
        <coordinates><xsl:value-of select="@longitude"/>,<xsl:value-of select="@latitude"/>,<xsl:value-of select="@altitude"/><xsl:text> </xsl:text>
          <xsl:value-of select="../sensornode[@node_id=$parent_id][@longitude][@latitude][@altitude]/@longitude"/>,<xsl:value-of select="../sensornode[@node_id=$parent_id][@longitude][@latitude][@altitude]/@latitude"/>,<xsl:value-of select="../sensornode[@node_id=$parent_id][@longitude][@latitude][@altitude]/@altitude"/></coordinates>
      </LineString>
    </Placemark>
  </xsl:for-each>
  </Folder>
</Document></kml>
</xsl:template>

</xsl:stylesheet>
