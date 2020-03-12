package io.fixprotocol.md.event.mutable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.MutableDetail;

public class DetailImpl extends ContextImpl implements MutableDetail {

  private final Map<String, String> properties = new LinkedHashMap<>();

  public DetailImpl() {
    this(EMPTY_CONTEXT, 0);
  }

  public DetailImpl(int level) {
    this(EMPTY_CONTEXT, level);
  }
  
  public DetailImpl(String[] keys) {
    this(keys, 0);
  }

  public DetailImpl(String[] keys, int level) {
    super(keys, level);
  }

  @Override
  public void addProperty(String key, String value) {
    properties.put(key.toLowerCase(), value);
  }

  @Override
  public Context getContext() {
    return this;
  }

  public Stream<Entry<String, String>> getProperties() {
    return properties.entrySet().stream();
  }

  @Override
  public String getProperty(String key) {
    return properties.get(key.toLowerCase());
  }

  @Override
  public String toString() {
    return "DetailImpl [properties=" + properties + ", getKeys()=" + Arrays.toString(getKeys())
        + ", getLevel()=" + getLevel() + "]";
  }

}
