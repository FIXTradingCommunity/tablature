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

import io.fixprotocol.md.event.mutable.ContextImpl;
import io.fixprotocol.md.event.mutable.DetailImpl;
import io.fixprotocol.md.event.mutable.DetailTableImpl;
import io.fixprotocol.md.event.mutable.DocumentationImpl;

public class ContextFactory {
  public MutableContext createContext(int level) {
    return new ContextImpl(level);
  }

  public MutableContext createContext(String[] keys, int level) {
    return new ContextImpl(keys, level);
  }

  public MutableContextual createDetail() {
    return new DetailImpl();
  }

  public MutableDetailTable createDetailTable() {
    return new DetailTableImpl();
  }

  public MutableDocumentation createDocumentation(String documentation) {
    return new DocumentationImpl(documentation);
  }

}
