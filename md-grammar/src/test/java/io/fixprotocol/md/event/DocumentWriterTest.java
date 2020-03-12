package io.fixprotocol.md.event;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentWriterTest {

  private ContextFactory factory;
  private DocumentWriter documentWriter;
  private Writer writer;
  
  @BeforeEach
  void setUp() throws Exception {
    factory = new ContextFactory();
    writer = new StringWriter();
    documentWriter = new DocumentWriter(writer);
  }

  @Test
  void heading() throws IOException {
    String[] keys = new String[] {"Message", "NewOrderSingle", "scenario", "base"};
    MutableContext context = factory.createContext(keys , 1);
    documentWriter.write(context);
    String output = writer.toString();
    assertEquals("# Message NewOrderSingle scenario base\n\n",  output);
  }
  
  @Test
  void documentation() throws IOException {
    String[] keys = new String[] {"Message", "NewOrderSingle", "scenario", "base"};
    MutableDocumentation context = factory.createDocumentation(keys , 1).documentation("This is a paragraph.");
    documentWriter.write(context);
    String output = writer.toString();
    assertEquals("# Message NewOrderSingle scenario base\n\nThis is a paragraph.\n\n",  output);
  }

  @Test
  void table() throws IOException {
    String[] keys = new String[] {"Message", "NewOrderSingle", "scenario", "base"};
    MutableDetailTable table = factory.createDetailTable(keys , 1);
    MutableDetailProperties detailProperties = table.newRow();
    detailProperties.addProperty("Name", "SecurityID");
    detailProperties.addProperty("Tag", "48");
    detailProperties.addProperty("Presence", "required");
    detailProperties = table.newRow();
    detailProperties.addProperty("Name", "SecurityIDSource");
    detailProperties.addProperty("Tag", "22");
    detailProperties.addProperty("Presence", "required");
    documentWriter.write((DetailTable)table);
    String output = writer.toString();
    System.out.print(output);
  }
}
