package io.fixprotocol.md.util;

public final class StringUtil {

  /**
   * Trim leading and trailing whitespace or pipe characters, leaving just the text 
   * within a markdown table cell.
   * 
   * @param str string to strip
   * @return a string without leading or trailing whitespace
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
  
  /**
   * Trim leading and trailing whitespace and convert any internal linefeeds to spaces
   * 
   * @param str string to strip
   * @return a string without leading or trailing whitespace
   */
  public static String stripWhitespace(String str) {
    if (str == null) {
      return null;
    }
    final int strLen = str.length();
    int end = strLen - 1;
    int begin = 0;

    while ((begin < strLen)
        && (str.charAt(begin) == ' ' || str.charAt(begin) == '\t' || str.charAt(begin) == '\r' || str.charAt(begin) == '\n')) {
      begin++;
    }
    while ((begin < end) && (str.charAt(end) == ' ' || str.charAt(end) == '\t' || str.charAt(end) == '\r' || str.charAt(end) == '\n')) {
      end--;
    }
    String str2 = ((begin > 0) || (end < strLen)) ? str.substring(begin, end + 1) : str;
    return str2.replace('\n', ' ');
  }
}
