package io.fixprotocol.md.event;

public interface Context {

  String[] EMPTY_CONTEXT = new String[0];
  int DEFAULT_LEVEL = 1;

  /**
   * An array of keywords. Position may be significant.
   * 
   * @return an array of key words or {@link #EMPTY_CONTEXT} if no keys are known
   */
  String[] getKeys();

  /**
   * 
   * @return outline level, 1-based
   */
  int getLevel();

  /**
   * Returns a key by its position
   * 
   * @param position position in a key array, 0-based
   * @return the key or {@code null} if position exceeds the size of the key array
   */
  String getKey(int position);

  /**
   * Returns a value assuming that context keys are formed as key-value pairs (possibly after
   * positional keys)
   * 
   * @param key key to match
   * @return the value after the matching key or {@code null} if the key is not found
   */
  String getKeyValue(String key);

}
