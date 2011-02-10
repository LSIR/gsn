<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema">
<xsl:template match="/">
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2"
xmlns:atom="http://www.w3.org/2005/Atom">
<Document>
  <xsl:for-each select="gsn/virtual-sensor">
  <PhotoOverlay>                                                                                                                                        
        <name><xsl:value-of select="@name"/></name>                                                                                                                     
        <Camera>
                <longitude><xsl:value-of select="field[@name='longitude']"/></longitude>                                                                                              
                <latitude><xsl:value-of select="field[@name='latitude']"/></latitude>                                                                                                
                <altitude><xsl:value-of select="field[@name='altitude']"/></altitude>                                                                                                
                <heading><xsl:value-of select="field[@name='heading']"/></heading>                                                                                                            
                <tilt><xsl:value-of select="field[@name='tilt']"/></tilt>
                <roll><xsl:value-of select="field[@name='roll']"/></roll>
                <altitudeMode>relativeToGround</altitudeMode>
                <gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>
        </Camera>
        <Style>
                <IconStyle>
                        <heading>0</heading>
                        <Icon>
                                <href>http://maps.google.com/mapfiles/kml/shapes/camera.png<!--camera_mode.png--></href>
                        </Icon>
                </IconStyle>
                <ListStyle>
                        <listItemType>check</listItemType>
                        <ItemIcon>
                                <state>open closed error fetching0 fetching1 fetching2</state>
                                <href>http://maps.google.com/mapfiles/kml/shapes/camera-lv.png</href>
                        </ItemIcon>
                        <bgColor>00ffffff</bgColor>
                        <maxSnippetLines>2</maxSnippetLines>
                </ListStyle>
        </Style>
        <Icon>
                <href><xsl:value-of select="field[@name='jpeg_scaled']"/></href>
        </Icon>
        <ViewVolume>
                <leftFov>-<xsl:value-of select="field[@name='horfov']"/></leftFov>
                <rightFov><xsl:value-of select="field[@name='horfov']"/></rightFov>
                <bottomFov>-<xsl:value-of select="field[@name='verfov']"/></bottomFov>
                <topFov><xsl:value-of select="field[@name='verfov']"/></topFov>
                <near><xsl:value-of select="field[@name='near']"/></near>
        </ViewVolume>
        <Point>
                <altitudeMode>relativeToGround</altitudeMode>
                <gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>
                <coordinates><xsl:value-of select="field[@name='longitude']"/>,<xsl:value-of select="field[@name='latitude']"/>,<xsl:value-of select="field[@name='altitude']"/></coordinates>
        </Point>
        <description>
        <xsl:value-of select="@description"/>
        </description>
  </PhotoOverlay>
  </xsl:for-each>
</Document></kml>
</xsl:template>

</xsl:stylesheet>
