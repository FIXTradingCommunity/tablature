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
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.Datatype;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.MessageType;

class Md2OrchestraTest {

  @BeforeAll
  public static void setupOnce() {
    new File(("target/test")).mkdirs();
  }

  private final Logger logger = LogManager.getLogger(getClass());

  
  @Test
  void firstNoReference() throws Exception {
    final String inputPath = getResourcePath("FIRST.md");
    final String outputFilename = "target/test/FIRST-noref.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath)
        .outputFile(outputFilename).eventFile("target/test/FIRST-noref.json").build();
    md2Orchestra1.generate();
  }
  
  @Test
  void firstWithReference() throws Exception {
    final String inputPath = getResourcePath("FIRST.md");
    final String outputFilename = "target/test/FIRST-ref.xml";
    String referencePath = getResourcePath("OrchestraFIXLatest.xml");
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath).referenceFile(referencePath)
        .outputFile(outputFilename).eventFile("target/test/FIRST-ref.json").build();
    md2Orchestra1.generate();
  }
  
  @Test
  void noInput() throws Exception {
    final String outputFilename = "target/test/mit_2016.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder()
        .outputFile(outputFilename).eventFile("target/test/mit_2016.json").build();
    assertThrows(IllegalArgumentException.class, md2Orchestra1::generate);
  }
  
  @Test
  void roundtrip() throws Exception {
    final String inputPath = getResourcePath("mit_2016.md");
    final String outputFilename = "target/test/mit_2016.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath)
        .outputFile(outputFilename).eventFile("target/test/mit_2016.json").build();
    md2Orchestra1.generate();
  }
  
  @Test
  void fixLatest() throws Exception {
    final String inputPath = getResourcePath("OrchestraFIXLatest.md");
    final String outputFilename = "target/test/OrchestraFIXLatest.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath)
        .outputFile(outputFilename).eventFile("target/test/OrchestraFIXLatest.json").build();
    md2Orchestra1.generate();
  }

  @Test
  void twoInputs() throws Exception {
    // glob should match 2 files
    String inputGlob = getResourcePath("md2orchestra-proto-p?.md");
    final String outputFilename = "target/test/md2orchestra-proto2.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputGlob)
        .outputFile(outputFilename).build();
    md2Orchestra1.generate();
 
    RepositoryAdapter outfile = new RepositoryAdapter(RepositoryBuilder.createEventListener(logger, null));
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

    FieldType field6234 = outfile.findFieldByTag(BigInteger.valueOf(6234), "base");
    assertNotNull(field6234);

    FieldType field6235 = outfile.findFieldByTag(BigInteger.valueOf(6235), "base");
    assertNotNull(field6235);
  }

  @Test
  void withoutReference() throws Exception {
    String inputPath = getResourcePath("md2orchestra-proto.md");
    final String outputFilename = "target/test/md2orchestra-proto.xml";
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath)
        .outputFile(outputFilename).build();
    md2Orchestra1.generate();

    RepositoryAdapter outfile = new RepositoryAdapter(RepositoryBuilder.createEventListener(logger, null));
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

    FieldType field6234 = outfile.findFieldByTag(BigInteger.valueOf(6234), "base");
    assertNotNull(field6234);

    FieldType field6235 = outfile.findFieldByTag(BigInteger.valueOf(6235), "base");
    assertNotNull(field6235);
  }

  @Test
  void withReference() throws Exception {
    String inputPath = getResourcePath("md2orchestra-proto.md");
    String outputFilename = "target/test/md2orchestra-proto-ref.xml";
    String referencePath = getResourcePath("OrchestraFIXLatest.xml");
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath).referenceFile(referencePath)
        .outputFile(outputFilename).build();
    md2Orchestra1.generate();
  }
  

  @Test
  void datatypes() throws Exception {
    String inputPath = getResourcePath("itiviti.md");
    final String outputFilename = "target/test/itiviti.xml";
    final String referencePath = getResourcePath("OrchestraFIXLatest.xml");
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFilePattern(inputPath).referenceFile(referencePath)
        .outputFile(outputFilename).build();
    md2Orchestra1.generate();

    RepositoryAdapter outfile = new RepositoryAdapter(RepositoryBuilder.createEventListener(logger, null));
    outfile.unmarshal(new FileInputStream(outputFilename));
    
    Datatype datatype = outfile.findDatatypeByName("Length");
    assertNotNull(datatype);
    
    datatype = outfile.findDatatypeByName("MultipleStringValue");
    assertNotNull(datatype);
    
    datatype = outfile.findDatatypeByName("Reserved100Plus");
    assertNotNull(datatype);
  }


  private String getResourcePath(String filename) {
    final File resourcesFile = new File("src/test/resources");
    final File file = new File(resourcesFile, filename);
    return file.getAbsolutePath();
  }

}
