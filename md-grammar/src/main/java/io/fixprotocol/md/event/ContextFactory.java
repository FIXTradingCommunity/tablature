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

  /**
   * Create markdown documentation as ordinary paragraphs
   *
   * @param documentation contents
   * @return a mutable documentation object
   */
  public MutableDocumentation createDocumentation(String documentation) {
    return new DocumentationImpl(documentation);
  }

  /**
   * Create markdown documentation as ordinary paragraphs
   *
   * @param documentation contents
   * @param format the format of the documentation. This corresponds to infostring of a fenced code
   *        block, as defined by the markdown specification. Originally, it was the name of a
   *        programming language to support syntax-specific highlighting. However, it has been
   *        extended to support various encodings, such as XML. In some cases, it may map to a media
   *        type.
   * @return a mutable documentation object
   */
  public MutableDocumentation createDocumentation(String documentation, String format) {
    return new DocumentationImpl(documentation, format);
  }

}
