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

public final class StringUtil {

  public static String convertToTitleCase(String key) {
    final StringBuilder sb = new StringBuilder();
    sb.append(Character.toUpperCase(key.charAt(0)));
    for (int i = 1; i < key.length(); i++) {
      if (Character.isWhitespace(key.charAt(i - 1))) {
        sb.append(Character.toUpperCase(key.charAt(i)));
      } else {
        sb.append(Character.toLowerCase(key.charAt(i)));
      }
    }
    return sb.toString();
  }


}
