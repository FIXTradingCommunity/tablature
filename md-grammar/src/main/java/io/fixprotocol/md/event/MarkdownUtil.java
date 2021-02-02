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

public final class MarkdownUtil {

  public static final String MARKDOWN_MEDIA_TYPE = "text/markdown";

  /**
   * Translate plaintext to markdown
   *
   * <ul>
   * <li>Escape these characters: pipe '|'</li>
   * <li>Convert internal line break to a space</li>
   * <li>Pass through XML/HTML entity references</li>
   * </ul>
   *
   * @param text plaintext
   * @return a markdown string
   */
  public static String plainTextToMarkdown(String text) {
    final StringBuilder sb = new StringBuilder(text.length());
    final String stripped = text.strip();
    for (int i = 0; i < stripped.length(); i++) {
      final char c = stripped.charAt(i);
      switch (c) {
        case '|':
          sb.append('\\');
          sb.append(c);
          break;
        case '\n':
          sb.append(' ');
          break;
        default:
          sb.append(c);
      }

    }
    return sb.toString();
  }

  /**
   * Trim leading and trailing whitespace or pipe characters, leaving just the text within a
   * markdown table cell.
   *
   * @param str string to strip
   * @return a string without leading or trailing whitespace, or {@code null} if the parameter is
   *         null
   */
  public static String stripCell(String str) {
    if (str == null) {
      return null;
    }
    final int strLen = str.length();
    int end = strLen - 1;
    int begin = 0;

    while ((begin < strLen)
        && (str.charAt(begin) == ' ' || str.charAt(begin) == '\t' || str.charAt(begin) == '|')) {
      begin++;
    }
    while ((begin < end) && (str.charAt(end) == ' ' || str.charAt(end) == '\t')) {
      end--;
    }
    return ((begin > 0) || (end < strLen)) ? str.substring(begin, end + 1) : str;
  }
}
