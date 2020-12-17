/*
 * Copyright 2020 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
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
    MutableContext context = factory.createContext(keys, 1);
    documentWriter.write(context);
    String output = writer.toString();
    assertEquals("# Message NewOrderSingle scenario base\n\n", output);
  }

  @Test
  void documentation() throws IOException {
    MutableDocumentation documentation = factory.createDocumentation("This is a paragraph.");
    documentWriter.write(documentation);
    String output = writer.toString();
    assertEquals("This is a paragraph.\n\n", output);
  }
  
  @Test
  void codeblock() throws IOException {
    MutableDocumentation documentation = factory.createDocumentation("<Instrmt ID=\"HGZ0\" src=\"ExchangeSymbol\" />", "xml");
    documentWriter.write(documentation);
    String output = writer.toString();
    assertEquals("```xml\n<Instrmt ID=\"HGZ0\" src=\"ExchangeSymbol\" />```\n\n", output);
  }

  @Test
  void table() throws IOException {
    MutableDetailTable table = factory.createDetailTable();
    MutableDetailProperties detailProperties = table.newRow();
    detailProperties.addProperty("Name", "SecurityID");
    detailProperties.addProperty("Tag", "48");
    detailProperties.addProperty("Presence", "required");
    detailProperties = table.newRow();
    detailProperties.addProperty("Name", "SecurityIDSource");
    detailProperties.addProperty("Tag", "22");
    detailProperties.addProperty("Presence", "required");
    documentWriter.write(table);
    String output = writer.toString();
    System.out.print(output);
  }
}
