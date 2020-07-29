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

import java.util.Collection;
import java.util.Map.Entry;


/**
 * Key-value properties
 *
 * @author Don Mendelson
 *
 */
public interface DetailProperties {

  /**
   * Access a integer property by its key
   *
   * @param key key to the property
   * @return value of the property, or {@code null} if the property does not exist or is non-numeric
   */
  Integer getIntProperty(String key);

  /**
   * An unmodifiable Collection of key-value pairs
   *
   * @return a Collection of entries
   */
  Collection<Entry<String, String>> getProperties();

  /**
   * Access a property by its key
   *
   * @param key key to the property
   * @return value of the property, or {@code null} if the property does not exist
   */
  String getProperty(String key);


}
