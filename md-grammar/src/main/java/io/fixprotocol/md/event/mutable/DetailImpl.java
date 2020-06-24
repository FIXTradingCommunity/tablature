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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.MutableDetail;

public class DetailImpl extends ContextImpl implements MutableDetail {

  private final Map<String, String> properties = new LinkedHashMap<>();

  public DetailImpl() {
    this(EMPTY_CONTEXT, 0);
  }

  public DetailImpl(int level) {
    this(EMPTY_CONTEXT, level);
  }

  public DetailImpl(String[] keys) {
    this(keys, 0);
  }

  public DetailImpl(String[] keys, int level) {
    super(keys, level);
  }

  @Override
  public void addIntProperty(String key, int value) {
    addProperty(key, Integer.toString(value));
  }

  @Override
  public void addProperty(String key, String value) {
    properties.put(key.toLowerCase(), value);
  }

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public Integer getIntProperty(String key) {
    final String property = getProperty(key);
    if (property != null) {
      try {
        return Integer.valueOf(property);
      } catch (final NumberFormatException e) {
        return null;
      }
    } else
      return null;
  }

  @Override
  public Stream<Entry<String, String>> getProperties() {
    return properties.entrySet().stream();
  }

  @Override
  public String getProperty(String key) {
    return StringUtil.stripCell(properties.get(key.toLowerCase()));
  }

  @Override
  public String toString() {
    return "DetailImpl [properties=" + properties + ", getKeys()=" + Arrays.toString(getKeys())
        + ", getLevel()=" + getLevel() + "]";
  }

}
