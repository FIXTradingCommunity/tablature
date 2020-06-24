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
package io.fixprotocol.md2interfaces;

import java.io.File;
import java.io.FileOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Md2InterfacesTest {

  @BeforeAll
  public static void setupOnce() {
    new File(("target/test")).mkdirs();
  }
  
  @Test
  void testMain() throws Exception {
    Md2Interfaces md2Interfaces = new Md2Interfaces();
    md2Interfaces.generate(Thread.currentThread().getContextClassLoader().getResourceAsStream("SampleInterfaces.md"), 
        new FileOutputStream("target/test/SampleInterfaces.xml"));
  }

}
