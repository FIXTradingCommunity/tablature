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
    MarkdownGenerator generator = new MarkdownGenerator();
    generator.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("SampleInterfaces.xml"),
        new OutputStreamWriter(new FileOutputStream("target/test/SampleInterfaces.md"), StandardCharsets.UTF_8),
        new FileOutputStream("target/test/SampleInterfaces.json"));
  }

}
