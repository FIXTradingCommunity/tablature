package io.fixprotocol.md.event;

import java.util.Map.Entry;
import java.util.stream.Stream;


/**
 * Key-value properties
 *
 * @author Don Mendelson
 *
 */
public interface DetailProperties {

  /**
   *
   * @return the Context of this collection of properties
   */
  Context getContext();

  /**
   * Access a integer property by its key
   * 
   * @param key key to the property
   * @return value of the property, or {@code null} if the property does not exist or is non-numeric
   */
  Integer getIntProperty(String key);

  /**
   * A Stream of key-value pairs
   * 
   * @return a Stream of entries
   */
  Stream<Entry<String, String>> getProperties();

  /**
   * Access a property by its key
   * 
   * @param key key to the property
   * @return value of the property, or {@code null} if the property does not exist
   */
  String getProperty(String key);


}
