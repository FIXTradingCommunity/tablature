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

import java.io.File;
import java.io.FileOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
  @Test
  void mainWithReference() throws Exception {
    final String args[] = new String[] {"-i", "md2orchestra-proto.md", "-o", "target/test/main1.xml",
        "-r", "FixRepository50SP2EP247.xml", "-e", "target/test/main1-err.txt"};
    Md2Orchestra.main(args);
  }

  
  @Disabled
  @Test
  void builder() throws Exception {
    Md2Orchestra md2Orchestra1 = Md2Orchestra.builder().inputFile("md2orchestra-proto.md")
        .outputFile("target/test/builder1.xml").build();
    md2Orchestra1.generate();

  }
  
  @Test
  void roundtrip() throws Exception {
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    md2Orchestra.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("mit_2016.md"),
        new FileOutputStream("target/test/mit_2016.xml"), null);
  }
  
  @Test
  void withReference() throws Exception {
    Md2Orchestra md2Orchestra = new Md2Orchestra();
    md2Orchestra.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("md2orchestra-proto.md"), 
        new FileOutputStream("target/test/md2orchestra-proto.xml"), 
        Thread.currentThread().getContextClassLoader().getResourceAsStream("FixRepository50SP2EP247.xml"));
  }

}
