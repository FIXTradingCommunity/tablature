package io.fixprotocol.md2orchestra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryBuilderTest {

  private OutputStream jsonOutputStream;
  
  @BeforeEach
  void setUp() throws Exception {
    jsonOutputStream = new ByteArrayOutputStream(8096);
  }

  @Test // ODOC-49
  void badCodes() throws Exception {
    String text =
        "### Codeset SideCodeSet type char\n\n"+
        "| Name | Values | Documentation |\n"+
        "|------------------|:-------:|---------------|\n"+
        "| Buy | 1 | |\n"+
        "| | 2 | |\n"+
        "| Undisclosed | 7 | |\n";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = new RepositoryBuilder(jsonOutputStream);
    builder.appendInput(inputStream);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    assertTrue(errors.contains("Missing value"));
    assertTrue(errors.contains("Missing name"));
  }
  
  @Test //ODOC-42
  void unknownTagForFieldScenario() throws Exception {
    String text =
        "## Message NewOrderSingle type D\n"
        + "\n"
        + "| Name | Tag | Presence | Scenario |\n"
        + "|----------------|----:|-----------|--------------|\n"
        + "| ClOrdID | | required | |\n"
        + "| Instrument | c | | |\n"
        + "| Side | | required | BuySell |\n"
        + "\n"
        + "### Codeset SideCodeSet type char scenario BuySell\n"
        + "\n"
        + "| Name | Value | Documentation |\n"
        + "|------------------|:-------:|---------------|\n"
        + "| Buy | 1 | |\n"
        + "| Sell | 2 | |\n";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertFalse(errors.contains("Unknown fieldRef ID"));
  }
  
  @Test //ODOC-45
  void redundantField() throws Exception {
    String text =
        "## Component Instrument\n"
        + "\n"
        + "| Name | Tag | Presence | Values | Scenario |\n"
        + "|------------------|----:|-----------|--------|--------------|\n"
        + "| SecurityID | 48 | required | | |\n"
        + "| SecurityIDSource | 22 | required | | SecIDSources |\n"
        + "| SecurityStatus | 965 | | | |";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    String xml = xmlStream.toString();
    //System.out.println(xml);
    Pattern fieldPattern = Pattern.compile("<fixr:field\\s.+?id=\"22\".*?>");
    Matcher fieldMatcher = fieldPattern.matcher(xml);
    long count = fieldMatcher.results().count();
    assertEquals(1, count);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test //ODOC-23
  void brokenConstant() throws Exception {
    String text =
        "## Component Instrument scenario test\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | |\n"
        + "| SecurityStatus | 965 | | 1 |\n";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Missing value for constant"));
  }
  
  @Test //ODOC-22
  // TODO: currently "Active firm" with space is not valid in Orchestra, but consider changing that
  void inlineCode() throws Exception {
    String text =
        "## Component Instrument scenario test\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | |\n"
        + "| SecurityStatus | 965 | | 1=Active firm |\n";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Missing value for constant"));
    assertFalse(errors.contains("Malformed inline code"));
  }
  
  @Test //ODOC-22
  void spaceInCodeName() throws Exception {
    String text =
        "### Codeset SecIDSources type String\n"
        + "\n"
        + "| Name | Value | Documentation |\n"
        + "|------------------|:-------:|---------------|\n"
        + "| CUSIP | 1 | Explaining CUSIP |\n"
        + "| ISIN | 4 | ISINs *are* great |\n"
        + "| Exchange symbol | 8 | **Good** as well |";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-60
  void userDefinedColumns() throws Exception {
    String text =
        "## Component Instrument\n"
        + "\n"
        + "| Name | Tag | Presence | Values | Notes |\n"
        + "|------------------|----:|-----------|--------|---------|\n"
        + "| SecurityID | 48 | required | | blabla \n"
        + "| SecurityIDSource | 22 | | |\n"
        + "| SecurityStatus | 965 | | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = new RepositoryBuilder(jsonOutputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.appendInput(inputStream);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(xml.contains("appinfo purpose=\"notes\">blabla"));
  }

}
