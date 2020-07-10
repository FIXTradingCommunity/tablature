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
import java.net.URI;
import java.net.URL;
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
    final String args[] = new String[] {"-o", outputFilename,
        "-r", "FixRepository50SP2EP247.xml", "-e", "target/test/main1-err.txt",
        "md2orchestra-proto.md", };
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
    URL url = Thread.currentThread().getContextClassLoader().getResource("mit_2016.md");
    URI uri = url.toURI();
    File inputFile = new File(uri);
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    md2Orchestra.generate(List.of(inputFile), new File("target/test/mit_2016.xml"), null,
        new File("target/test/mit_2016.log"));
  }
  
  @Test
  void withoutReference() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource("md2orchestra-proto.md");
    URI uri = url.toURI();
    File inputFile = new File(uri);
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    final String outFilename = "target/test/md2orchestra-proto.xml";
    md2Orchestra.generate(List.of(inputFile), new File(outFilename), null,
        new File("target/test/md2orchestra-proto.log"));

    RepositoryAdapter outfile = new RepositoryAdapter();
    outfile.unmarshal(new FileInputStream(outFilename));
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
  void twoInputs() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource("md2orchestra-proto-p1.md");
    URI uri = url.toURI();
    File inputFile1 = new File(uri);
    url = Thread.currentThread().getContextClassLoader().getResource("md2orchestra-proto-p2.md");
    uri = url.toURI();
    File inputFile2 = new File(uri);
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    final String outFilename = "target/test/md2orchestra-proto2.xml";
    md2Orchestra.generate(List.of(inputFile1, inputFile2), new File(outFilename), null,
        new File("target/test/md2orchestra-proto2.log"));

    RepositoryAdapter outfile = new RepositoryAdapter();
    outfile.unmarshal(new FileInputStream(outFilename));
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
    URL url = Thread.currentThread().getContextClassLoader().getResource("md2orchestra-proto.md");
    URI uri = url.toURI();
    File inputFile = new File(uri);
    
    url = Thread.currentThread().getContextClassLoader().getResource("FixRepository50SP2EP247.xml");
    uri = url.toURI();
    File referenceFile = new File(uri);
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    final String outFilename = "target/test/md2orchestra-proto-ref.xml";
    md2Orchestra.generate(List.of(inputFile), new File(outFilename), referenceFile,
        new File("target/test/md2orchestra-proto-ref.log"));
 }

}
