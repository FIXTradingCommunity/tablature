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
  
  @Test // ODOC-43
  void badConstant() throws Exception {
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
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Missing value for constant"));
  }
  
  @Test // ODOC-23
  void badInlineCodes() throws Exception {
    String text =
        "## Component Instrument scenario test\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | 4 |\n"
        + "| SecurityStatus | 965 | | 1=Active 2 |";   
    
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
    assertTrue(errors.contains("Malformed inline code"));
  }

  @Test // ODOC-26
  void badMetadata() throws Exception {
    String text =
        "# FIX.5.0SP2 FIX.5.0SP2_EP216\n"
        + "\n"
        + "| Term       | Value                                 |\n"
        + "|------------|---------------------------------------|\n"
        + "| title      | Orchestra Example                     |\n"
        + "| creator    | Millennium IT                         |\n"
        + "| publisher  | FIX Trading Community                 |\n"
        + "| rights     | Copyright 2019, FIX Protocol, Limited |\n"
        + "| date       | 2019-01-09T16:09:16.904-06:00         |\n"
        + "| version    | 2.1.3                |";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = RepositoryBuilder.instance(null , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("invalid metadata term version"));
  }
  
  @Test //ODOC-20
  void constant() throws Exception {
    String text =
        "## Component Instrument\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | 8 |\n";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(xml.contains("value=\"8\""));
  }
  
  @Test // ODOC-77
  void documentationColumns() throws Exception {
    String text =
        "### Codeset SecurityIDSourceCodeSet\n"
        + "\n"
        + "| Name | Value | Synopsis | Elaboration |\n"
        + "|------------------|:-------:|-----------------|-------------|\n"
        + "| IndexName | W | Index name | Standard name of the index or rate index, e.g. \"LIBOR\" or \"iTraxx Australia. |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(xml.contains("SYNOPSIS"));
    assertTrue(xml.contains("ELABORATION"));
  }
  
  @Test // ODOC-98
  void documentationParagraph() throws Exception {
    String text =
        "### Codeset PartyRoleCodeSet\n"
        + "\n"
        + "Identifies the type or role of the PartyID (448) specified.\n"
        + "\n"
        + "| Name | Value | Documentation |\n"
        + "|------------------|:-------:|---------------|\n"
        + "| ExecutingFirm | 1 | |\n"
        + "| ClearingFirm | 4 | |\n"
        + "| ExecutingTrader | 7 | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-99
  void documentationParagraphNoPurpose() throws Exception {
    String text =
        "### Codeset SecIDSources type String (22)\n"
        + "Domains for values of SecurityID(48)\n"
        + "Second line";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = RepositoryBuilder.instance(null, jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertFalse(xml.contains("purpose="));
  }
  @Test // ODOC-63
  void duplicateCodes() throws Exception {
    String text =
        "### Codeset SideCodeSet scenario BuySell type char\n"
        + "\n"
        + "| Name | Value | Documentation |\n"
        + "|------------------|:-------:|---------------|\n"
        + "| Buy | 1 | |\n"
        + "| Sell | 2 | |\n"
        + "\n"
        + "### Codeset SideCodeSet type char scenario BuySell\n"
        + "\n"
        + "| Name | Value | Documentation |\n"
        + "|------------------|:-------:|---------------|\n"
        + "| Buy | 1 | |\n"
        + "| Sell | 2 | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Duplicate definitions of codeset"));
   }
  
  @Test // ODOC-66
  void duplicateCodesets() throws Exception {
    String text =
        "## Component Instrument\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|----------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | | |\n"
        + "| SecurityStatus | 965 | | 1=Active |\n"
        + "\n"
        + "## Component Instrument scenario Test\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|----------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | | |\n"
        + "| SecurityStatus | 965 | | 1=Active |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Duplicate definitions of codeset"));
   }
  
  @Test // ODOC-31
  void emptyComponent() throws Exception {
    String text =
        "## Component InstrumentParties\n"
        + "\n";
    
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
  
  @Test // ODOC-30
  void extraComponentWithSynopsis() throws Exception {
    String text =
        "## Component Instrument\n"
        + "### Synopsis\n"
        + "The `Instrument` component block contains all the fields commonly used to describe a security or instrument.\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | |\n"
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
    Pattern pattern = Pattern.compile("<fixr:component ");
    Matcher matcher = pattern.matcher(xml);
    long count = matcher.results().count();
    assertEquals(1, count);
  }
  
  @Test // ODOC-100
  void groupNoNumInGroup() throws Exception {
    String text =
        "### Group Parties category MyCategory (1012)\n"
        + "\n"
        + "| Name | Tag | Presence |\n"
        + "|---------|-----|----------|\n"
        + "| PartyID | 448 | required |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("First group field not NumInGroup datatype"));
  }
  
  @Test // ODOC-100
  void groupNoNumInGroup2() throws Exception {
    String text =
        "## Group Parties category MyCategory abbrName Pty\n"
        + "\n"
        + "| Name | Tag | Presence |\n"
        + "|----------------|----:|:---------:|\n"
        + "| PartyID | | r |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("First group field not NumInGroup datatype"));
  }
  
  @Test
  void inlineCode() throws Exception {
    String text =
        "## Component Instrument scenario test\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | |\n"
        + "| SecurityStatus | 965 | | 1=Active |\n";   
    
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
    assertTrue(xml.contains("name=\"SecurityStatusCodeset\""));
    assertTrue(xml.contains("type=\"SecurityStatusCodeSet\""));
    assertFalse(errors.contains("Malformed inline code"));
  }
  
  @Test // ODOC-24
  void inlineCodes() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name             | Tag | Type         |  Values                  |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| OrdType          | 40  | char         | 1=Market 2=Limit         |";
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
    assertTrue(xml.contains("name=\"Market\""));
    assertTrue(xml.contains("name=\"Limit\""));
  }
  
  @Test //ODOC-22
  // TODO: currently "Active firm" with space is not valid in Orchestra, but consider changing that
  void inlineCodeWithSpace() throws Exception {
    String text =
        "## Component Instrument scenario test\n"
        + "\n"
        + "| Name | Tag | Presence | Values |\n"
        + "|------------------|----:|-----------|--------|\n"
        + "| SecurityID | 48 | required | |\n"
        + "| SecurityIDSource | 22 | constant | |\n"
        + "| SecurityStatus | 965 | | 1=\"Active firm\" |\n";   
    
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
    assertTrue(xml.contains("name=\"Active firm\""));
    assertTrue(errors.contains("Missing value for constant"));
    assertFalse(errors.contains("Malformed inline code"));
  }
  
  @Test // ODOC-86
  void lookupDatatypes() throws Exception {
    String text =
        "## Fields\n"
        + "| Name             | Tag | Type         |  Values                  |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| Account | 1 | | |\n"
        + "| AvgPx | | Price | |\n"
        + "| BeginSeqNo | | | |\n";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    builder.closeEventLogger();
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    String xml = xmlStream.toString();
    //System.out.println(xml);
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    Pattern pattern = Pattern.compile("<fixr:datatype ");
    Matcher matcher = pattern.matcher(xml);
    long count = matcher.results().count();
    assertEquals(3, count);
  }
  
  @Test // ODOC-40
  void missingFieldId() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name | Tag | Type | Values |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| Account | | String | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.appendInput(inputStream);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(xml.contains("name=\"Account\""));
  }
  
  @Test // ODOC-40
  void missingFieldName() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name | Tag | Type | Values |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| | 1 | String | |\n";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.appendInput(inputStream);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(xml.contains("name=\"Account\""));
  }
  
  @Test //ODOC-19
  void missingFieldTag() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name | Tag | Type | Values |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| Account | 1 | String | |\n"
        + "| ClOrdID | | String | |\n"
        + "| NoParties | 453 | NumInGroup | |\n"
        + "| OrderQty | 38 | String | |\n";   
    
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
  
  @Test
  void missingFieldType() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name | Tag | Type | Values |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| Account | 1 | String | |\n"
        + "| ClOrdID | | String | |\n"
        + "| NoParties | 453 | NumInGroup | |\n"
        + "| OrderQty | 38 |  | |\n";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    String xml = xmlStream.toString();
    assertTrue(xml.contains("type=\"Qty\""));
    // System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-78
  void optionalPresence() throws Exception {
    String text =
        "## Message NewOrderSingle\n"
        + "\n"
        + "| Name | Tag | Presence |\n"
        + "|----------------|----:|-------------------------|\n"
        + "| ClOrdID | | required |\n"
        + "| Account | | |\n"
        + "| Instrument | c | |\n"
        + "| Parties | g | |";   
    
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
  
  @Test // ODOC-74
  void paragraphBreak() throws Exception {
    String text =
        "## Fields\n\n"
        + "| Name | Tag | Type | Synopsis | Values |\n"
        + "|------------------|----:|--------------|--------------------------|--------|\n"
        + "| SecurityID | 48 | String | Line 1/P/Line 2 |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = new RepositoryBuilder(jsonOutputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.appendInput(inputStream);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    assertTrue(xml.contains("\n\n"));
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-32, ODOC-58
  void parseError() throws Exception {
    String text =
        "## Component InstrumentParties\n"
        + "\n"
        + "|\n" // deliberate error
        + "| Name | Value | Documentation | My Doc |\n"
        + "|------------------|:-------:|---------------|-------|\n"
        + "| Buy | 1 | | Xyz ABC\\ |\n"
        + "| Sell | 2 | | |\n"
        + "| Undisclosed | 7 | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = new RepositoryBuilder(jsonOutputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.appendInput(inputStream);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    // String errors = jsonOutputStream.toString();
    // System.out.println(errors);
    assertTrue(xml.contains("name=\"InstrumentParties\""));
  }
  
  @Test //ODOC-45
  void redundantField() throws Exception {
    String text = "### Codeset SecIDSources type String (22)\n"
        + "\n"
        + "Domains for values of SecurityID(48)\n"
        + "\n"
        + "| Name | Value | Documentation |\n"
        + "|------------------|:-------:|---------------|\n"
        + "| CUSIP | 1 | Explaining CUSIP |\n"
        + "| ISIN | 4 | ISINs *are* great |\n"
        + "| Exchange symbol | 8 | **Good** as well |\n"
        + "\n"
        + "## Component Instrument\n"
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
    Pattern pattern = Pattern.compile("name=\"SecurityIDSource\"");
    Matcher matcher = pattern.matcher(xml);
    long count = matcher.results().count();
    assertEquals(1, count);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-98
  void replaceDocumentation() throws Exception {
    String text =
        "### Codeset PartyRoleCodeSet\n"
        + "#### Synopsis\n"
        + "Hanno: Identifies the type or role of the PartyID (448) specified.";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    Pattern pattern = Pattern.compile("purpose=\"SYNOPSIS\"");
    Matcher matcher = pattern.matcher(xml);
    long count = matcher.results().count();
    assertEquals(1, count);
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @BeforeEach
  void setUp() throws Exception {
    jsonOutputStream = new ByteArrayOutputStream(8096);
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
  
  @Test // ODOC-21
  void unknownId() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name | Tag | Type | Values |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| MyUserDefined1 | 6234| UTCTimestamp | |\n"
        + "| MyUserDefined2 | 6235| | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = RepositoryBuilder.instance(null , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Unknown type for field"));
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
  
  @Test // ODOC-60, ODOC-55
  void userDefinedColumns() throws Exception {
    String text =
        "## Component Instrument\n"
        + "\n"
        + "| Name | Tag | Presence | Values | Notes |\n"
        + "|------------------|----:|-----------|--------|---------|\n"
        + "| SecurityID | 48 | required | | blabla \n"
        + "| SecurityIDSource | 22 | | |\n"
        + "| SecurityStatus | 965 | | |\n"
        + "| OptionExercise | component | required | | yada yada \n"
        + "| EvntGrp | group | required | | nonsense \n";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    InputStream referenceStream = new FileInputStream("src/test/resources/OrchestraFIXLatest.xml");
    RepositoryBuilder builder = RepositoryBuilder.instance(referenceStream , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(xml.contains("appinfo purpose=\"notes\">blabla"));
    assertTrue(xml.contains("appinfo purpose=\"notes\">yada yada"));
    assertTrue(xml.contains("appinfo purpose=\"notes\">nonsense"));
  }
  
  @Test // ODOC-97
  void userDefinedFields() throws Exception {
    String text =
        "## Fields\n"
        + "\n"
        + "| Name | Tag | Type | Values |\n"
        + "|------------------|----:|--------------|--------------------------|\n"
        + "| MyUserDefined1 | 6234| UTCTimestamp | |\n"
        + "| MyUserDefined2 | 6235| | |";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    RepositoryBuilder builder = RepositoryBuilder.instance(null , jsonOutputStream);
    builder.appendInput(inputStream);
    ByteArrayOutputStream xmlStream = new ByteArrayOutputStream(8096);
    builder.write(xmlStream);
    builder.closeEventLogger();
    //String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Unknown type for field"));
  }

}
