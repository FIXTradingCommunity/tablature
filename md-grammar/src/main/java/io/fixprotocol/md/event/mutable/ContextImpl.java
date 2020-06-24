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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.MutableContext;

public class ContextImpl implements MutableContext {

  private final List<String> keys = new ArrayList<>();
  private final int level;
  private Context parent = null;

  public ContextImpl() {
    this(EMPTY_CONTEXT, DEFAULT_LEVEL);
  }

  public ContextImpl(int level) {
    this(EMPTY_CONTEXT, level);
  }

  public ContextImpl(String[] keys) {
    this(keys, DEFAULT_LEVEL);
  }

  public ContextImpl(String[] keys, int level) {
    this.keys.addAll(Arrays.asList(keys));
    this.level = level;
  }

  @Override
  public void addKey(String key) {
    keys.add(key);
  }

  /**
   * Returns a key by its position
   *
   * @param position position in a key array, 0-based
   * @return the key or {@code null} if position exceeds the size of the key array
   */
  @Override
  public String getKey(int position) {
    if (keys.size() > position) {
      return keys.get(position);
    } else {
      return null;
    }
  }

  @Override
  public String[] getKeys() {
    final String[] a = new String[keys.size()];
    return keys.toArray(a);
  }

  /**
   * Returns a value assuming that context keys are formed as key-value pairs (possibly after
   * positional keys)
   *
   * @param key key to match
   * @return the value after the matching key or {@code null} if the key is not found
   */
  @Override
  public String getKeyValue(String key) {
    for (int i = 0; i < keys.size() - 1; i++) {
      if (keys.get(i).equalsIgnoreCase(key)) {
        return keys.get(i + 1);
      }
    }
    return null;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public Context getParent() {
    return parent;
  }

  @Override
  public void setParent(Context parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return "ContextImpl [keys=" + keys + ", level=" + level + "]";
  }

}
