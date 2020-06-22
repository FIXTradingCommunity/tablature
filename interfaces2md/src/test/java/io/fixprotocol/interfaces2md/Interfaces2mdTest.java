package io.fixprotocol.interfaces2md;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Interfaces2mdTest {
  
  @BeforeAll
  public static void setupOnce() {
    new File(("target/test")).mkdirs();
  }

  @Test
  void exampleWithStreams() throws Exception {
    Interfaces2md interfaces2md = new Interfaces2md();
    interfaces2md.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("SampleInterfaces.xml"), 
        new OutputStreamWriter(new FileOutputStream("target/test/SampleInterfaces.md"), StandardCharsets.UTF_8));
  }

}
