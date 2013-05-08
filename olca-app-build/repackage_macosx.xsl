<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

   <xsl:output indent="yes" method="xml" doctype-system="http://www.apple.com/DTDs/PropertyList-1.0.dtd" doctype-public="-//Apple Computer//DTD PLIST 1.0//EN"/>

   <xsl:param name="launcher"/>
   <xsl:param name="launcher.library"/>

   <xsl:template match="/">
      <xsl:apply-templates/>
   </xsl:template>

   <xsl:template match="array[preceding-sibling::key[text()='Eclipse']]">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
         <!-- currently the plugin system needs -clean to work reliably -->
         <string>-clean</string>
         <string>-startup</string>
         <string>$APP_PACKAGE/plugins/<xsl:value-of select="$launcher"/></string>
         <string>--launcher.library</string>
         <string>$APP_PACKAGE/plugins/<xsl:value-of select="$launcher.library"/></string>
      </xsl:copy>

      <key>WorkingDirectory</key>
      <string>$APP_PACKAGE/</string>
   </xsl:template>

   <xsl:template match="processing-instruction()|comment()">
      <xsl:copy/>
   </xsl:template>

   <!-- copy -->
   <xsl:template match="*">
      <xsl:copy>
         <!-- go process attributes and children -->
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="@*">
      <xsl:copy-of select="."/>
   </xsl:template>

</xsl:stylesheet>
