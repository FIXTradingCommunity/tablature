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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.MarkdownUtil;
import io.fixprotocol.md.event.MutableDetail;

public class DetailImpl implements MutableDetail {

  private final Map<String, String> properties = new LinkedHashMap<>();
  private Context parent;


  @Override
  public void addIntProperty(String key, int value) {
    addProperty(key, Integer.toString(value));
  }

  @Override
  public void addProperty(String key, String value) {
    properties.put(key.toLowerCase(), value);
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
  public Context getParent() {
    return parent;
  }

  @Override
  public Collection<Entry<String, String>> getProperties() {
    return Collections.unmodifiableSet(properties.entrySet());
  }

  @Override
  public String getProperty(String key) {
    return MarkdownUtil.stripCell(properties.get(key.toLowerCase()));
  }

  @Override
  public void setParent(Context parent) {
    this.parent = parent;
  }


}
