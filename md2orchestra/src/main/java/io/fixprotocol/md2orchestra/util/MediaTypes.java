package io.fixprotocol.md2orchestra.util;

import io.fixprotocol.md.event.MarkdownUtil;

public final class MediaTypes {

  /**
   * Translates some values of Markdown infostring (block format) to a media type
   * 
   * @param format infostring of a fenced code block in Markdown
   * @return a media type registered with IANA
   * @see <a href="https://github.com/github/linguist/blob/master/lib/linguist/languages.yml">Formats in GitHub</a>
   * @see <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">Media Types</a>
   */
  public static String formatToMediaType(String format) {
    switch (format) {
      case "markdown":
        return MarkdownUtil.MARKDOWN_MEDIA_TYPE;
      case "xml":
        return "application/xml";
      case "json":
        return "application/json";
      case "html":
        return "text/html";
      default:
        return "text/plain";
    }
  }

}
