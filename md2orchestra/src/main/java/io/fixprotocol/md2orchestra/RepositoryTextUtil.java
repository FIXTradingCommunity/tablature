package io.fixprotocol.md2orchestra;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Text formatting for repository elements
 *
 * @author Don Mendelson
 *
 */
class RepositoryTextUtil {
  private final Logger logger = LogManager.getLogger(getClass());

  /* String brackets from code names as it was sometimes written */
  String stripName(String str) {
    final int beginIndex = str.indexOf('[');
    final int endIndex = str.lastIndexOf(']');
    return str.substring(beginIndex >= 0 ? beginIndex + 1 : 0,
        endIndex >= 0 ? endIndex : str.length());
  }

  /* Strip parentheses from tag */
  int tagToInt(String str) {
    if (str == null) {
      return -1;
    }
    final int strLen = str.length();
    int end = strLen - 1;
    int begin = 0;

    while ((begin < strLen) && (str.charAt(begin) == ' ' || str.charAt(begin) == '\t'
        || str.charAt(begin) == '|' || str.charAt(begin) == '(')) {
      begin++;
    }
    while ((begin < end) && (str.charAt(end) == ' ' || str.charAt(end) == ')')) {
      end--;
    }
    final String str2 = ((begin > 0) || (end < strLen)) ? str.substring(begin, end + 1) : str;

    if (str2.isEmpty()) {
      return -1;
    } else {
      try {
        return Integer.parseInt(str2);
      } catch (final NumberFormatException e) {
        logger.trace("RepositoryTextUtil numeric tag value expected, was {}", str);
        return -1;
      }
    }
  }

}
