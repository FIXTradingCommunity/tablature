package io.fixprotocol.md.event;



public interface MutableDetailProperties extends DetailProperties {

  /**
   * Adds a key-value pair
   *
   * @param key key to the property
   * @param value value of the property as an integer
   */
  void addIntProperty(String key, int value);

  /**
   * Adds a key-value pair
   *
   * @param key key to the property
   * @param value value of the property as a String
   */
  void addProperty(String key, String value);

}
