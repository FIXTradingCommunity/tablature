package io.fixprotocol.md.event;

public interface MutableContext extends Context {

  /**
   * Add a key to the Context
   * 
   * @param key a key
   */
  void addKey(String key);

  /**
   * Add a pair of keys to the Context that may be interpreted as a key-value pair
   * 
   * @param key a key to the Context
   * @param value a value associated with the key
   */
  default void addPair(String key, String value) {
    addKey(key);
    addKey(value);
  }
  
  /**
   * Set a parent Context to build a hierarchy
   * @param parent parent Context
   */
  void setParent(Context parent);
}
