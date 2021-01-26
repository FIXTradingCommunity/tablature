package io.fixprotocol.md2orchestra;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryBuilderTest {

  private RepositoryBuilder builder;
  private OutputStream jsonOutputStream;
  
  @BeforeEach
  void setUp() throws Exception {
    jsonOutputStream = new ByteArrayOutputStream(8096);
    builder = new RepositoryBuilder(jsonOutputStream);
  }

  @Test
  void badCodes() throws IOException {
    String text =
        "### Codeset SideCodeSet type char\n\n"+
        "| Name | Values | Documentation |\n"+
        "|------------------|:-------:|---------------|\n"+
        "| Buy | 1 | |\n"+
        "| | 2 | |\n"+
        "| Undisclosed | 7 | |\n";
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    builder.appendInput(inputStream);
    String errors = jsonOutputStream.toString();
  }

}
