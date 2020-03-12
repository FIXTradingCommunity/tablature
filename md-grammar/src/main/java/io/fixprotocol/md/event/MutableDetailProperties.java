package io.fixprotocol.md.event;



public interface MutableDetailProperties extends DetailProperties {

  /**
   * Adds a key-value pair
   * @param key key to the property
   * @param value value of the property
   */
  void addProperty(String key, String value);

}
