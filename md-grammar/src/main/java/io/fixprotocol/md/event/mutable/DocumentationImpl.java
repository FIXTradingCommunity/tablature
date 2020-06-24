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
package io.fixprotocol.md.event.mutable;

import java.util.Arrays;
import io.fixprotocol.md.event.MutableDocumentation;

public class DocumentationImpl extends ContextImpl implements MutableDocumentation {

  private String documentation;

  public DocumentationImpl(int level) {
    super(EMPTY_CONTEXT, level);
  }

  public DocumentationImpl(String documentation) {
    this(EMPTY_CONTEXT, 0, documentation);
  }

  public DocumentationImpl(String[] keys, int level) {
    super(keys, level);
  }

  public DocumentationImpl(String[] keys, int level, String documentation) {
    super(keys, level);
    this.documentation = documentation;
  }

  public DocumentationImpl(String[] keys, String documentation) {
    this(keys, 0, documentation);
  }

  @Override
  public MutableDocumentation documentation(String documentation) {
    this.documentation = documentation;
    return this;
  }

  @Override
  public String getDocumentation() {
    return documentation;
  }

  @Override
  public String toString() {
    return "DocumentationImpl [documentation=" + documentation + ", getKeys()="
        + Arrays.toString(getKeys()) + ", getLevel()=" + getLevel() + "]";
  }

}
