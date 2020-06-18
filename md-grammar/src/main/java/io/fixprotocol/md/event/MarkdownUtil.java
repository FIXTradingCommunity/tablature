package io.fixprotocol.md.event;

public final class MarkdownUtil {
  
  public static final String MARKDOWN_MEDIA_TYPE = "text/markdown";

  /**
   * Translate plaintext to markdown
   * 
   * <ul>
   * <li>Escape these characters: pipe '|'</li>
   * <li>Convert internal line break to a space</li>
   * <li>Pass through XML/HTML entity references/li>
   * </ul>
   * 
   * @param text plaintext
   * @return a markdown string
   */
  public static String plainTextToMarkdown(String text) {
    final StringBuilder sb = new StringBuilder(text.length());
    final String stripped = text.strip();
    for (int i = 0; i < stripped.length(); i++) {
      char c = stripped.charAt(i);
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

}
