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
package io.fixprotocol.orchestra2md;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class Orchestra2mdTest {

  @BeforeAll
  public static void setupOnce() {
    new File(("target/test")).mkdirs();
  }
  
  @Test
  void main() throws Exception {
    Orchestra2md.main(new String[] {"-o", "target/test/md2orchestra-proto.md", "-e", "target/test/md2orchestra-proto.json", 
        "src/test/resources/md2orchestra-proto.xml", });
  }
  
  @Disabled
  @Test
  void badOrchestra() throws Exception {
    Orchestra2md.main(new String[] {"-o", "target/test/badOrchestra.md", "-e", "target/test/badOrchestra.json", 
        "src/test/resources/badOrchestra.xml", });
  }
  
  @Test
  void message() throws Exception {
    Orchestra2md orchestra2md = Orchestra2md.builder().inputFile("src/test/resources/message.xml")
        .outputFile("target/test/message.md").eventFile("target/test/message.json")
        .pedigree(true).datatypes(false).build();
    orchestra2md.generate();
  }
  
  @Test
  void FixLatest() throws Exception {
    Orchestra2md orchestra2md = Orchestra2md.builder().inputFile("src/test/resources/OrchestraFIXLatest.xml")
        .outputFile("target/test/OrchestraFIXLatest.md").eventFile("target/test/OrchestraFIXLatest.json")
        .pedigree(true).datatypes(true).build();
    orchestra2md.generate();
  }
  
  @Disabled
  @Test
  void roundtripWithStreams() throws Exception {
    MarkdownGenerator generator = new MarkdownGenerator();
    generator.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("roundtrip.xml"), 
        new OutputStreamWriter(new FileOutputStream("target/test/roundtrip.md"), StandardCharsets.UTF_8), new FileOutputStream("target/test/roundtrip.json"));
  }
  
  @Test
  void exampleWithStreams() throws Exception {
    MarkdownGenerator generator = new MarkdownGenerator();
    generator.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("mit_2016.xml"), 
        new OutputStreamWriter(new FileOutputStream("target/test/mit_2016.md"), StandardCharsets.UTF_8), new FileOutputStream("target/test/mit_2016.json"));
  }
}
