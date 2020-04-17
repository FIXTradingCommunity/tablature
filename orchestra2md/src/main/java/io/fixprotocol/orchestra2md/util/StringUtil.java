package io.fixprotocol.orchestra2md.util;

public final class StringUtil {
  
  /**
   * Translate plaintext to markdown
   * 
   * Escape these characters: pipe
   * Pass through XML/HTML entity references
   * 
   * @param text plaintext
   * @return a markdown string
   */
  public static String plainTextToMarkdown(String text) {
    final StringBuilder sb = new StringBuilder(text.length());
    final String stripped = text.strip();
    for (int i=0; i<stripped.length(); i++) {
      char c = stripped.charAt(i);
      if (c == '|') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

}
