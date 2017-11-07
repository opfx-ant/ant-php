<!--
  This stylesheet is for work in build.xml:printFailingTests.
  It extracts all failing tests from a given testsuite (JUnit+AntUnit XML format)
  and writes that into a text file.
  All text files are written to STDOUT via <concat>.
-->
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- Output format so no XML header would be written -->
    <xsl:output indent="no" method="text" encoding="ISO-8859-1"/>
    <!-- What is the name of the current testsuite (JUnit class or AntUnit buildfile) -->
    <xsl:variable name="testsuite" select="/testsuite/@name"/>


<!-- failing tests: suitename.testname : message; Leading pipe for line break -->
<xsl:template match="testcase[failure|error]">
| <xsl:value-of select="$testsuite"/>.<xsl:value-of select="@name"/>() : <xsl:value-of select="failure/@message"/><xsl:value-of select="error/@message"/>
</xsl:template>

<!-- Suppress log output from the tests like stacktraces -->
<xsl:template match="text()"/>


</xsl:stylesheet>