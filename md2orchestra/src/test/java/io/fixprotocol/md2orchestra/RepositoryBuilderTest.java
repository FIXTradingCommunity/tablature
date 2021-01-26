package io.fixprotocol.md2orchestra;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
    String xml = xmlStream.toString();
    //System.out.println(xml);
    builder.closeEventLogger();
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertFalse(errors.contains("Unknown fieldRef ID"));
  }

}
