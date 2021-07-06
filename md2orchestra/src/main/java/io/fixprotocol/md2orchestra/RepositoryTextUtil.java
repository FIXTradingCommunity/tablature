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

  /**
   * Return a tag in the form described by {@link #tagToInt(String)}
   * 
   * @param strings an array of strings
   * @return a tag, or {@code -1} if not found
   */
  int getTag(final String[] strings) {
    for (final String str : strings) {
      final int tag = tagToInt(str);
      if (tag != -1) {
        return tag;
      }
    }
    return -1;
  }

  /**
   * Strip optional brackets from code name
   * 
   * @param str string containing a code name in the form {@code [name]}
   * @return the name without brackets
   */
  String stripName(final String str) {
    final int beginIndex = str.indexOf('[');
    final int endIndex = str.lastIndexOf(']');
    return str.substring(beginIndex >= 0 ? beginIndex + 1 : 0,
        endIndex >= 0 ? endIndex : str.length());
  }

  /**
   * Strip expected parentheses from tag to get number
   * 
   * @param str a string in the form {@code (999)}
   * @return an integer extracted from the string, or {@code -1} if the value is non-numeric
   */
  int tagToInt(final String str) {
    final int beginIndex = str.indexOf('(');
    final int endIndex = str.lastIndexOf(')');
    if (beginIndex == -1 || endIndex == -1) {
      return -1;
    }

    final String str2 = str.substring(beginIndex+1, endIndex);

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
