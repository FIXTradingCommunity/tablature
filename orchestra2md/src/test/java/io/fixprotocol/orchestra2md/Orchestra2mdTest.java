package io.fixprotocol.orchestra2md;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class Orchestra2mdTest {

  @BeforeAll
  public static void setupOnce() {
    new File(("target/test")).mkdirs();
  }
  
  @Disabled
  @Test
  void ep() throws Exception {
    Orchestra2md orchestra2md = Orchestra2md.builder().inputFile("FixRepository50SP2EP247.xml").outputFile("target/test/FixRepository50SP2EP247.md").build();
    orchestra2md.generate();
  }

  @Disabled
  @Test
  void roundtrip() throws Exception {
    Orchestra2md orchestra2md = Orchestra2md.builder().inputFile("roundtrip.xml").outputFile("target/test/roundtrip.md").build();
    orchestra2md.generate();
  }
  
  @Test
  void roundtripWithStreams() throws Exception {
    Orchestra2md orchestra2md = new Orchestra2md();
    orchestra2md.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("roundtrip.xml"), 
        new OutputStreamWriter(new FileOutputStream("target/test/roundtrip.md"), StandardCharsets.UTF_8));
  }
}
