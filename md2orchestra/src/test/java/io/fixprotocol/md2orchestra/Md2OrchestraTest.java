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
package io.fixprotocol.md2orchestra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.MessageType;

class Md2OrchestraTest {
  
  @BeforeAll
  public static void setupOnce() {
    new File(("target/test")).mkdirs();
  }

  /**
   * Invoke Md2Orchestra main entry point with command line parameters
   * 
   * Specifies input markdown file, output Orchestra file, and a reference Orchestra file 
   * 
   * Equivalent to command line:
   * <pre>
   * java io.fixprotocol.md2orchestra.Md2Orchestra -i "md2orchestra-proto.md"" -o target/test/main1.xml -r FixRepository50SP2EP247.xml -e target/test/main1-err.txt
   * </pre>
   */
  @Disabled
  @Test
  void mainWithReference() throws Exception {
    final String outputFilename = "target/test/main1.xml";
    final String args[] = new String[] {"-i", "md2orchestra-proto.md", "-o", outputFilename,
        "-r", "FixRepository50SP2EP247.xml", "-e", "target/test/main1-err.txt"};
    Md2Orchestra.main(args);
  }

  @Disabled
  @Test
  void builder() throws Exception {
    final String outputFilename = "target/test/builder1.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFile("md2orchestra-proto.md")
        .outputFile(outputFilename).build();
    md2Orchestra1.generate();
  }
  
  @Test
  void roundtrip() throws Exception {
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    md2Orchestra.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("mit_2016.md"),
        new FileOutputStream("target/test/mit_2016.xml"), null);
  }
  
  @Test
  void withoutReference() throws Exception {
    final String outputFilename = "target/test/md2orchestra-proto.xml";
    Md2Orchestra md2Orchestra = new Md2Orchestra();
     md2Orchestra.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("md2orchestra-proto.md"), 
        new FileOutputStream(outputFilename), null);
    RepositoryAdapter outfile = new RepositoryAdapter();
    outfile.unmarshal(new FileInputStream(outputFilename));
    MessageType message = outfile.findMessageByName("NewOrderSingle", "base");
    assertNotNull(message);

    CodeSetType codeset = outfile.findCodesetByName("SideCodeSet", "base");
    assertNotNull(codeset);
    List<CodeType> codes = codeset.getCode();
    assertEquals(4, codes.size());
    
    ComponentType component = outfile.findComponentByName("Instrument", "base");
    assertNotNull(component);
    List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
    assertEquals(2, members.size());
    
    GroupType group = outfile.findGroupByName("Parties", "base");
    assertNotNull(group);
    List<Object> groupMembers = group.getComponentRefOrGroupRefOrFieldRef();
    assertEquals(453, group.getNumInGroup().getId().intValue());
    assertEquals(3, groupMembers.size());
    
    FieldType field6234 = outfile.findFieldByTag(6234, "base");
    assertNotNull(field6234);
    
    FieldType field6235 = outfile.findFieldByTag(6235, "base");
    assertNotNull(field6235);
  }
  
  @Test
  void withReference() throws Exception {
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    md2Orchestra.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("md2orchestra-proto.md"), 
        new FileOutputStream("target/test/md2orchestra-proto-ref.xml"), 
        Thread.currentThread().getContextClassLoader().getResourceAsStream("FixRepository50SP2EP247.xml"));
  }

}
