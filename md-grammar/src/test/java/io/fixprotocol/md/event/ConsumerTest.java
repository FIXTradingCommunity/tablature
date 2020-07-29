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

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ConsumerTest {

  @ParameterizedTest
  @ValueSource(strings = {"md2orchestra-proto.md"})
  void consume(String fileName) throws IOException {
    Consumer<Contextual> contextConsumer = new Consumer<>() {

      @Override
      public void accept(Contextual contextual) {
        Context parent = contextual.getParent();
        if (parent != null) {
          final String[] parentKeys = parent.getKeys();
          System.out.format("Parent context=%s level=%d%n",
              parentKeys.length > 0 ? parentKeys[0] : "None", parent.getLevel());
        }
        if (contextual instanceof Detail) {
          Detail detail = (Detail) contextual;
          detail.getProperties().forEach(property -> System.out.format("Property key=%s value=%s%n",
              property.getKey(), property.getValue()));
        } else if (contextual instanceof Documentation) {
          Documentation documentation = (Documentation) contextual;
          System.out.format("Documentation %s%n", documentation.getDocumentation());
        } else if (contextual instanceof Context) {
          Context context = (Context) contextual;
          final String[] keys = context.getKeys();
          System.out.format("Context=%s level=%d%n", keys.length > 0 ? keys[0] : "None",
              context.getLevel());
        }

      }
    };

    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
    DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, contextConsumer);
  }

}
