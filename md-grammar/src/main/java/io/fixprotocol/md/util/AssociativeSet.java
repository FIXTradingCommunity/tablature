package io.fixprotocol.md.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Association of a pair of values supports lookup by either value.
 *
 * Each value in an association must be unique.
 *
 * Not thread-safe.
 *
 * @author Don Mendelson
 *
 * @param <T> value type
 */
public class AssociativeSet {

  private final Map<String, String> firstkey = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private final Map<String, String> secondkey = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  /**
   * Add values to this AssociativeSet
   *
   * @param v1 first key value
   * @param v2 second key value
   * @return {@code true} if this set did not already contain the specified element
   */
  public boolean add(final String v1, final String v2) {
    if (firstkey.get(v1) == null && secondkey.get(v2) == null) {
      firstkey.put(v1, v2);
      secondkey.put(v2, v1);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Add values to this AssociativeSet
   *
   * @param values a array of two-element arrays
   * @return {@code true} if this set did not already contain the specified elements
   */
  public boolean addAll(String[][] values) {
    boolean isUnique = true;
    for (final String[] pair : values) {
      if (pair.length >= 2) {
        isUnique = isUnique && add(pair[0], pair[1]);
      }
    }
    return isUnique;
  }

  public void clear() {
    firstkey.clear();
    secondkey.clear();
  }

  /**
   * Access an association by the first key
   *
   * @param v1 value to search
   * @return the second value of an association or {@code null} if not found
   */
  public String get(final String v1) {
    return firstkey.get(v1);
  }
  
  /**
   * Access an association by the first key
   * @param v1 value to search
   * @param defaultValue value to return if no association found
   * @return the second value of an association or {@code defaultValue} if not found
   */
  public String getOrDefault(String v1, String defaultValue) {
    String v2 = firstkey.get(v1);
    return v2 != null ? v2 : defaultValue;
  }

  /**
   * Access an association by the second key
   *
   * @param v2 value to search
   * @return the first value of an association or {@code null} if not found
   */
  public String getSecond(final String v2) {
    return secondkey.get(v2);
  }
  
  /**
   * Access an association by the second key
   * @param v2 value to search
   * @param defaultValue value to return if no association found
   * @return the first value of an association or {@code defaultValue} if not found
   */
  public String getSecondOrDefault(String v2, String defaultValue) {
    String v1 = secondkey.get(v2);
    return v1 != null ? v1 : defaultValue;
  }

  public int size() {
    return firstkey.size();
  }

  /**
   * Returns all values in the set
   *
   * Returned as array of Object because it is not possible to create generic arrays in Java.
   *
   * @return a List of two-element array containing key values
   */
  public List<Object[]> values() {
    return firstkey.entrySet().stream().map(e -> new Object[] {e.getKey(), e.getValue()})
        .collect(Collectors.toList());
  }

}
