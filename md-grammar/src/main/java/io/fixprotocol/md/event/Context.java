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

/**
 * A semantic context for subsequent contents
 */
public interface Context extends Contextual {

  int DEFAULT_LEVEL = 1;
  String[] EMPTY_CONTEXT = new String[0];

  /**
   * Returns a key by its position
   *
   * @param position position in a key array, 0-based
   * @return the key or {@code null} if position exceeds the size of the key array
   */
  String getKey(int position);

  /**
   * An array of keywords. Position may be significant.
   *
   * @return an array of key words or {@link #EMPTY_CONTEXT} if no keys are known
   */
  String[] getKeys();

  /**
   * Returns a value assuming that context keys are formed as key-value pairs (possibly after
   * positional keys)
   *
   * @param key key to match
   * @return the value after the matching key or {@code null} if the key is not found
   */
  String getKeyValue(String key);

  /**
   *
   * @return outline level, 1-based
   */
  int getLevel();

}
