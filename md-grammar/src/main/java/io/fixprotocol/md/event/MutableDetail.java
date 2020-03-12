package io.fixprotocol.md.event;

public interface MutableDetail extends Detail, MutableContext {

  void addProperty(String key, String value);

}
