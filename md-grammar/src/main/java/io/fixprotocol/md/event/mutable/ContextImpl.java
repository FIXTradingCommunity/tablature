package io.fixprotocol.md.event.mutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import io.fixprotocol.md.event.MutableContext;

public class ContextImpl implements MutableContext {

  private final List<String> keys = new ArrayList<>();
  private final int level;

  public ContextImpl() {
    this(EMPTY_CONTEXT, DEFAULT_LEVEL);
  }

  public ContextImpl(int level) {
    this(EMPTY_CONTEXT, level);
  }

  public ContextImpl(String[] keys) {
    this(keys, DEFAULT_LEVEL);
  }

  public ContextImpl(String[] keys, int level) {
    this.keys.addAll(Arrays.asList(keys));
    this.level = level;
  }

  @Override
  public void addKey(String key) {
    keys.add(key);
  }

  /**
   * Returns a key by its position
   *
   * @param position position in a key array, 0-based
   * @return the key or {@code null} if position exceeds the size of the key array
   */
  @Override
  public String getKey(int position) {
    if (keys.size() > position) {
      return keys.get(position);
    } else {
      return null;
    }
  }

  @Override
  public String[] getKeys() {
    final String[] a = new String[keys.size()];
    return keys.toArray(a);
  }

  /**
   * Returns a value assuming that context keys are formed as key-value pairs (possibly after
   * positional keys)
   *
   * @param key key to match
   * @return the value after the matching key or {@code null} if the key is not found
   */
  @Override
  public String getKeyValue(String key) {
    for (int i = 0; i < keys.size() - 1; i++) {
      if (keys.get(i).equalsIgnoreCase(key)) {
        return keys.get(i + 1);
      }
    }
    return null;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public String toString() {
    return "ContextImpl [keys=" + keys + ", level=" + level + "]";
  }

}
